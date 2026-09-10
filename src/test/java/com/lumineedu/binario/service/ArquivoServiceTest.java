package com.lumineedu.binario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.repository.ArquivoRepository;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.exception.ArquivoNaoEncontradoException;
import com.lumineedu.binario.service.ArquivoStorageService.ArquivoArmazenado;
import com.lumineedu.binario.service.ArquivoStorageService;
import com.lumineedu.binario.testutil.ImagemTeste;

/**
 * Testes unitarios do servico de negocio de arquivos, utilizando
 * Mockito para simular a camada de persistencia.
 */
@ExtendWith(MockitoExtension.class)
class ArquivoServiceTest {

    @Mock
    private ArquivoRepository arquivoRepository;

    @Mock
    private ArquivoStorageService arquivoStorageService;

    @InjectMocks
    private ArquivoService arquivoService;

    @Test
    void deveCriarArquivoComConteudoBase64Valido() {
        // Preparacao
        ImagemTeste.ImagemCriacao imagem = ImagemTeste.criarBase64("foto.jpg", 10, 10, Color.WHITE, "png");
        ArquivoDTO dto = new ArquivoDTO("foto.jpg", "image/png", "descricao do arquivo", imagem.conteudoBase64);
        ArquivoArmazenado armazenado = new ArquivoArmazenado("foto.jpg", "/armazenamento/arquivos/foto.jpg", 100);
        Arquivo arquivo = new Arquivo();
        arquivo.setId(1L);
        when(arquivoStorageService.salvar(dto)).thenReturn(armazenado);
        when(arquivoRepository.save(any(Arquivo.class))).thenReturn(arquivo);

        // Acao
        ArquivoResponse resposta = arquivoService.criar(dto);

        // Assert
        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNomeOriginal()).isEqualTo("foto.jpg");
        assertThat(resposta.getTamanhoBytes()).isEqualTo(100L);
        verify(arquivoRepository, times(1)).save(any(Arquivo.class));
    }

    @Test
    void deveLancarExcecaoQuandoArquivoNaoEncontrado() {
        // Preparacao
        when(arquivoRepository.findById(999L)).thenReturn(Optional.empty());

        // Acao e assert
        assertThatThrownBy(() -> arquivoService.buscarPorId(999L))
                .isInstanceOf(ArquivoNaoEncontradoException.class);
    }

    @Test
    void deveRetornarArquivoExistentePorId() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setId(5L);
        arquivo.setNomeOriginal("origem.png");
        arquivo.setNomeArquivo("origem.png");
        arquivo.setTipoMime("image/png");
        arquivo.setDescricao("descricao");
        arquivo.setTamanhoBytes(200L);
        arquivo.setCaminhoRelativo("arquivos/origem.png");
        arquivo.setCaminhoFisico("/armazenamento/arquivos/origem.png");
        when(arquivoRepository.findById(5L)).thenReturn(Optional.of(arquivo));

        // Acao
        ArquivoResponse resposta = arquivoService.buscarPorId(5L);

        // Assert
        assertThat(resposta.getId()).isEqualTo(5L);
        assertThat(resposta.getCaminhoFisico()).isEqualTo("/armazenamento/arquivos/origem.png");
    }

    @Test
    void deveRemoverArquivoDoSistema() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setId(2L);
        arquivo.setNomeOriginal("remover.jpg");
        arquivo.setCaminhoFisico("/armazenamento/arquivos/remover.jpg");
        when(arquivoRepository.findById(2L)).thenReturn(Optional.of(arquivo));

        // Acao
        ArquivoResponse resposta = arquivoService.remover(2L);

        // Assert
        assertThat(resposta.getId()).isEqualTo(2L);
        verify(arquivoStorageService, times(1)).removerFisico("/armazenamento/arquivos/remover.jpg");
        verify(arquivoRepository, times(1)).delete(any(Arquivo.class));
    }

    @Test
    void deveLancarExcecaoAoRemoverArquivoInexistente() {
        // Preparacao
        when(arquivoRepository.findById(777L)).thenReturn(Optional.empty());

        // Acao e assert
        assertThatThrownBy(() -> arquivoService.remover(777L))
                .isInstanceOf(ArquivoNaoEncontradoException.class);
        verify(arquivoRepository, never()).delete(any(Arquivo.class));
    }

    @Test
    void deveListarArquivosComPaginacao() {
        // Preparacao
        Arquivo arquivo = novoArquivo("lista.png");
        Page<Arquivo> paginaArquivos = new PageImpl<>(List.of(arquivo), PageRequest.of(0, 10), 1L);
        when(arquivoRepository.findAll(any(Pageable.class))).thenReturn(paginaArquivos);

        // Acao
        Page<ArquivoResponse> pagina = arquivoService.listar(any(Pageable.class));

        // Assert
        assertThat(pagina.getTotalElements()).isEqualTo(1L);
        assertThat(pagina.getContent()).hasSize(1);
    }

    private static Arquivo novoArquivo(String nome) {
        Arquivo arquivo = new Arquivo();
        arquivo.setId(1L);
        arquivo.setNomeOriginal(nome);
        arquivo.setNomeArquivo(nome);
        arquivo.setTipoMime("image/png");
        arquivo.setDescricao("descricao");
        arquivo.setTamanhoBytes(100L);
        arquivo.setCaminhoRelativo("arquivos/" + nome);
        arquivo.setCaminhoFisico("/armazenamento/arquivos/" + nome);
        return arquivo;
    }
}
