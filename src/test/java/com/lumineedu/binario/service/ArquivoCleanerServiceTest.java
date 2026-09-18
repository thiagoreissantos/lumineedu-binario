package com.lumineedu.binario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lumineedu.binario.config.StorageProperties;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.repository.ArquivoRepository;

/**
 * Testes unitarios do Job de limpeza periodica de arquivos binarios.
 * Simula a camada de persistencia e de armazenamento com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ArquivoCleanerServiceTest {

    @Mock
    private ArquivoRepository arquivoRepository;

    @Mock
    private ArquivoStorageService arquivoStorageService;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private ArquivoCleanerService arquivoCleanerService;

    @Test
    void deveRemoverArquivosFisicosEDesativarRegistrosCandidatos() {
        // Preparacao: dois arquivos candidatos (ativos e sem acesso recente) e
        // um que nao deveria ser candidato (ativo, mas acessado ha pouco tempo).
        Arquivo candidatoUm = novoArquivoAtivoRecente(1L, "arquivo1.png", "/arm/arquivo1.png");
        candidatoUm.setUltimaLeitura(Instant.now().minusSeconds(2 * 365L * 24 * 60 * 60));
        Arquivo candidatoDois = novoArquivoAtivoRecente(2L, "arquivo2.png", "/arm/arquivo2.png");
        candidatoDois.setUltimaLeitura(null);
        Arquivo naoCandidato = novoArquivoAtivoRecente(3L, "arquivo3.png", "/arm/arquivo3.png");
        naoCandidato.setUltimaLeitura(Instant.now());
        List<Arquivo> candidatos = List.of(candidatoUm, candidatoDois);
        when(arquivoRepository.encontrarCandidatosAPesquisa(any(Instant.class))).thenReturn(candidatos);
        when(storageProperties.getMaxIdleDias()).thenReturn(730L);
        when(storageProperties.getHoraExecucaoLimpeza()).thenReturn(3);

        // Acao
        List<Arquivo> desativados = arquivoCleanerService.executar();

        // Assert
        assertThat(desativados).hasSize(2);
        verify(arquivoStorageService, times(2)).removerFisico(anyString());
        verify(arquivoRepository, times(1)).saveAll(candidatos);
        assertThat(desativados.get(0).isAtivo()).isFalse();
        assertThat(desativados.get(0).getDataDesativacao()).isNotNull();
        assertThat(desativados.get(1).isAtivo()).isFalse();
        assertThat(desativados.get(1).getDataDesativacao()).isNotNull();
    }

    @Test
    void devePreservarArquivosInativosJaProcessados() {
        // Preparacao: arquivo ja inativo (processado em execucao anterior) nao
        // deve aparecer entre os candidatos.
        Arquivo jaInativo = novoArquivoAtivoRecente(10L, "inativo.png", "/arm/inativo.png");
        jaInativo.setAtivo(false);
        jaInativo.setUltimaLeitura(Instant.now().minusSeconds(2 * 365L * 24 * 60 * 60));
        when(arquivoRepository.encontrarCandidatosAPesquisa(any(Instant.class))).thenReturn(List.of(jaInativo));
        when(storageProperties.getMaxIdleDias()).thenReturn(730L);

        // Acao
        List<Arquivo> desativados = arquivoCleanerService.executar();

        // Assert: arquivo ja inativo nao deve ser removido nem re-desativado
        verify(arquivoStorageService, never()).removerFisico(anyString());
        verify(arquivoRepository, never()).saveAll(any());
        assertThat(desativados).isEmpty();
        assertThat(jaInativo.isAtivo()).isFalse();
        assertThat(jaInativo.getDataDesativacao()).isNull();
    }

    @Test
    void devePreservarCandidatosSemCaminhoFisicoValido() {
        // Preparacao: candidato sem caminho fisico (conteudo ja deletado) deve
        // ser preservado (nao desativado).
        Arquivo semCaminho = novoArquivoAtivoRecente(20L, "semcaminho.png", "");
        semCaminho.setCaminhoFisico("");
        when(arquivoRepository.encontrarCandidatosAPesquisa(any(Instant.class))).thenReturn(List.of(semCaminho));
        when(storageProperties.getMaxIdleDias()).thenReturn(730L);

        // Acao
        List<Arquivo> desativados = arquivoCleanerService.executar();

        // Assert
        verify(arquivoStorageService, never()).removerFisico(anyString());
        verify(arquivoRepository, never()).saveAll(any());
        assertThat(desativados).isEmpty();
        assertThat(semCaminho.isAtivo()).isTrue();
    }

    @Test
    void deveContinuarMesmoQueExclusaoFisicaFallir() {
        // Preparacao: exclusao fisica do primeiro candidato falha; o Job deve
        // ser resiliente e processar o restante.
        Arquivo falhou = novoArquivoAtivoRecente(30L, "falhou.png", "/arm/falhou.png");
        Arquivo ok = novoArquivoAtivoRecente(31L, "ok.png", "/arm/ok.png");
        when(arquivoRepository.encontrarCandidatosAPesquisa(any(Instant.class)))
                .thenReturn(List.of(falhou, ok));
        when(storageProperties.getMaxIdleDias()).thenReturn(730L);
        doNothing().when(arquivoStorageService).removerFisico("/arm/ok.png");
        doThrow(new RuntimeException("disco cheio"))
                .when(arquivoStorageService).removerFisico("/arm/falhou.png");

        // Acao
        List<Arquivo> desativados = arquivoCleanerService.executar();

        // Assert
        assertThat(desativados).hasSize(1);
        assertThat(desativados).containsOnly(ok);
        assertThat(ok.isAtivo()).isFalse();
        // O arquivo que falhou nao deve ser desativado (reavaliado na proxima execucao)
        assertThat(falhou.isAtivo()).isTrue();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaCandidatos() {
        // Preparacao: sem candidatos no banco.
        when(arquivoRepository.encontrarCandidatosAPesquisa(any(Instant.class))).thenReturn(List.of());
        when(storageProperties.getMaxIdleDias()).thenReturn(730L);

        // Acao
        List<Arquivo> desativados = arquivoCleanerService.executar();

        // Assert
        assertThat(desativados).isEmpty();
        verify(arquivoStorageService, never()).removerFisico(anyString());
        verify(arquivoRepository, never()).saveAll(any());
    }

    private static Arquivo novoArquivoAtivoRecente(Long id, String nome, String caminhoFisico) {
        Arquivo arquivo = new Arquivo();
        arquivo.setId(id);
        arquivo.setNomeOriginal(nome);
        arquivo.setCaminhoFisico(caminhoFisico);
        arquivo.setAtivo(true);
        return arquivo;
    }
}
