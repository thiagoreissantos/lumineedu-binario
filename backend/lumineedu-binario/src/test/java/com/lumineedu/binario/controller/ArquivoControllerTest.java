package com.lumineedu.binario.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;
import com.lumineedu.binario.service.ArquivoService;
import com.lumineedu.binario.exception.handler.GlobalExceptionHandler;

import org.springframework.http.ResponseEntity;

/**
 * Testes diretos do controller de criacao de arquivos.
 *
 * <p>Evita o stack de MockMvc (cujas classes de resultado servlet
 * {@code MockMvcRequestResultPart}, {@code MockMvcResultBean} e
 * {@code MockHttpServletResponse} nao estao disponiveis nesta versao do
 * spring-test) e testa o metodo {@link ArquivoController#criar} diretamente,
 * injetando o servico via Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ArquivoControllerTest {

    @Mock
    private ArquivoService arquivoService;

    @Mock
    private GlobalExceptionHandler handler;

    @InjectMocks
    private ArquivoController controller;

    @Test
    void deveRetornarCreatedComStatus201() {
        // Preparacao
        ArquivoResponse esperado = ArquivoResponse.builder()
                .id(1L)
                .nomeOriginal("foto.jpg")
                .nomeArquivo("foto_1.jpg")
                .caminhoRelativo("storage/foto_1.jpg")
                .caminhoFisico("storage/foto_1.jpg")
                .tamanhoBytes(42L)
                .tipoMime("image/png")
                .descricao("descricao")
                .build();
        when(arquivoService.criar(any(ArquivoDTO.class))).thenReturn(esperado);

        // Acao
        ResponseEntity<ArquivoResponse> resposta = controller.criar(dtoValido());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(CREATED);
        assertThat(resposta.getBody()).isEqualTo(esperado);
    }

    @Test
    void devePropagarArquivoInvalido() {
        // Preparacao
        when(arquivoService.criar(any(ArquivoDTO.class))).thenThrow(new ArquivoInvalidoException());

        // Acao e assert
        assertThatThrownBy(() -> controller.criar(dtoInvalido()))
                .isInstanceOf(ArquivoInvalidoException.class);
    }

    @Test
    void devePropagarArquivoJaExistente() {
        // Preparacao
        when(arquivoService.criar(any(ArquivoDTO.class))).thenThrow(new ArquivoJaExisteException("dup.png"));

        // Acao e assert
        assertThatThrownBy(() -> controller.criar(dtoValido()))
                .isInstanceOf(ArquivoJaExisteException.class);
    }

    @Test
    void devePropagarArquivoMaiorQueLimite() {
        // Preparacao
        when(arquivoService.criar(any(ArquivoDTO.class))).thenThrow(new TamanhoInvalidoException());

        // Acao e assert
        assertThatThrownBy(() -> controller.criar(dtoValido()))
                .isInstanceOf(TamanhoInvalidoException.class);
    }

    private ArquivoDTO dtoValido() {
        return new ArquivoDTO("foto.jpg", "image/png", "descricao do arquivo", "aGVsbG8=");
    }

    private ArquivoDTO dtoInvalido() {
        return new ArquivoDTO("video.mp4", "video/mp4", "descricao do arquivo", "aGVsbG8=");
    }
}
