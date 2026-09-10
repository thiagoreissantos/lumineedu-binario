package com.lumineedu.binario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lumineedu.binario.config.SecurityProperties;
import com.lumineedu.binario.config.StorageProperties;
import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.ArmazenamentoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;
import com.lumineedu.binario.testutil.ImagemTeste;

/**
 * Testes unitarios do servico de armazenamento fisico, com Mockito
 * para injetar as propriedades de configuracao.
 */
@ExtendWith(MockitoExtension.class)
class ArquivoStorageServiceTest {

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private SecurityProperties securityProperties;

    @InjectMocks
    private ArquivoStorageService arquivoStorageService;

    @TempDir
    Path diretorio;

    @Test
    void deveSalvarArquivoImagemValidoNoDisco() throws Exception {
        // Preparacao
        ImagemTeste.ImagemCriacao imagem = ImagemTeste.criarBase64("foto.jpg", 10, 10, Color.WHITE, "png");
        ArquivoDTO dto = new ArquivoDTO("foto.jpg", "image/png", "descricao", imagem.conteudoBase64);
        when(storageProperties.getDiretorioCompleto()).thenReturn(diretorio.toString());
        when(securityProperties.getMimeTypeosPermitidos()).thenReturn(List.of("image/png", "image/jpeg"));
        when(storageProperties.getMaxTamanhoBytes()).thenReturn(52428800L);
        when(storageProperties.getMaxTamanhoHdBytes()).thenReturn(1073741824L);

        // Acao
        ArquivoStorageService.ArquivoArmazenado resultado = arquivoStorageService.salvar(dto);

        // Assert
        assertThat(resultado.getNomeArquivo()).isEqualTo("foto.jpg");
        Path arquivoSalvo = diretorio.resolve("foto.jpg");
        assertThat(arquivoSalvo).exists();
        assertThat(Files.size(arquivoSalvo)).isGreaterThan(0);
    }

    @Test
    void deveRejeitarArquivoComTipoMimeNaoPermitido() {
        // Preparacao
        ImagemTeste.ImagemCriacao imagem = ImagemTeste.criarBase64("video.mp4", 10, 10, Color.WHITE, "png");
        ArquivoDTO dto = new ArquivoDTO("video.mp4", "video/mp4", "descricao", imagem.conteudoBase64);
        when(storageProperties.getDiretorioCompleto()).thenReturn(diretorio.toString());
        when(securityProperties.getMimeTypeosPermitidos()).thenReturn(List.of("image/png", "image/jpeg"));
        when(storageProperties.getMaxTamanhoBytes()).thenReturn(52428800L);

        // Acao e assert
        assertThatThrownBy(() -> arquivoStorageService.salvar(dto))
                .isInstanceOf(ArquivoInvalidoException.class);
    }

    @Test
    void deveRejeitarArquivoMaiorQueLimiteConfigurado() {
        // Preparacao
        ImagemTeste.ImagemCriacao imagem = ImagemTeste.criarBase64("grande.png", 4000, 4000, Color.WHITE, "png");
        ArquivoDTO dto = new ArquivoDTO("grande.png", "image/png", "descricao", imagem.conteudoBase64);
        long limite = imagem.tamanhoBytes - 10;
        when(storageProperties.getMaxTamanhoBytes()).thenReturn(limite);

        // Acao e assert
        assertThatThrownBy(() -> arquivoStorageService.salvar(dto))
                .isInstanceOf(TamanhoInvalidoException.class);
    }

    @Test
    void deveRejeitarArquivoJaExistente() throws Exception {
        // Preparacao
        ImagemTeste.ImagemCriacao imagem = ImagemTeste.criarBase64("dup.png", 10, 10, Color.WHITE, "png");
        ArquivoDTO dto = new ArquivoDTO("dup.png", "image/png", "descricao", imagem.conteudoBase64);
        when(storageProperties.getDiretorioCompleto()).thenReturn(diretorio.toString());
        when(storageProperties.getMaxTamanhoBytes()).thenReturn(52428800L);
        Files.write(diretorio.resolve("dup.png"), "conteudo existente".getBytes());

        // Acao e assert
        assertThatThrownBy(() -> arquivoStorageService.salvar(dto))
                .isInstanceOf(ArquivoJaExisteException.class);
    }

    @Test
    void deveGerarNomeUnicoQuandoArquivoExiste() throws Exception {
        // Preparacao
        when(storageProperties.getDiretorioCompleto()).thenReturn(diretorio.toString());
        Files.write(diretorio.resolve("foto.jpg"), "conteudo".getBytes());

        // Acao
        String nomeUnico = arquivoStorageService.gerarNomeArquivoUnico("foto.jpg");

        // Assert
        assertThat(nomeUnico).isEqualTo("foto_1.jpg");
    }

    @Test
    void deveRemoverArquivoFisicoDoDisco() throws Exception {
        // Preparacao
        Path arquivo = diretorio.resolve("arquivos/remover.jpg");
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, "conteudo".getBytes());

        // Acao
        arquivoStorageService.removerFisico(arquivo.toString());

        // Assert
        assertThat(Files.exists(arquivo)).isFalse();
    }

    @Test
    void deveNaoLancarExcecaoAoRemoverCaminhoInexistente() {
        // Acao e assert
        try {
            arquivoStorageService.removerFisico(diretorio.resolve("nao-existe.jpg").toString());
        } catch (Exception ex) {
            throw new AssertionError("removerFisico nao deve lancar excecao para um caminho inexistente", ex);
        }
    }
}
