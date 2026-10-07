package com.lumineedu.binario.exception.handler;

import java.time.Instant;
import java.util.stream.Collectors;

import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArquivoNaoEncontradoException;
import com.lumineedu.binario.exception.ArmazenamentoException;
import com.lumineedu.binario.exception.ExcecaoNegocioException;
import com.lumineedu.binario.exception.TokenInvalidoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;
import com.lumineedu.binario.exception.ErroResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleTokenInvalido(
            TokenInvalidoException ex) {

        return construirResposta(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoInvalido(
            ArquivoInvalidoException ex) {

        return construirResposta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(TamanhoInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleTamanhoInvalido(
            TamanhoInvalidoException ex) {

        return construirResposta(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(ArquivoJaExisteException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoJaExiste(
            ArquivoJaExisteException ex) {

        return construirResposta(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(ArquivoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoNaoEncontrado(
            ArquivoNaoEncontradoException ex) {

        return construirResposta(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(ArmazenamentoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArmazenamento(
            ArmazenamentoException ex) {

        return construirResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(ExcecaoNegocioException.class)
    public ResponseEntity<ErroResponse<Void>> handleExcecaoNegocio(
            ExcecaoNegocioException ex) {

        return construirResposta(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse<Void>> handleAcessoNegado(
            AccessDeniedException ex) {

        return construirResposta(
                HttpStatus.FORBIDDEN,
                "acesso nao autorizado",
                ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse<Void>> handleValidacao(
            MethodArgumentNotValidException ex) {

        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatarErroCampo)
                .collect(Collectors.joining(", "));

        return construirResposta(
                HttpStatus.BAD_REQUEST,
                mensagem,
                ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse<Void>> handleGeral(Exception ex) {

        log.error("Erro inesperado ao processar requisicao", ex);

        String detalhe = ex.getClass().getSimpleName();

        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            detalhe += ": " + ex.getMessage();
        }

        return construirResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ocorreu um erro inesperado",
                detalhe,
                ex);
    }

    private ResponseEntity<ErroResponse<Void>> construirResposta(
            HttpStatus status,
            String mensagem,
            Exception ex) {

        return construirResposta(
                status,
                mensagem,
                ex.getMessage(),
                ex);
    }

    private ResponseEntity<ErroResponse<Void>> construirResposta(
            HttpStatus status,
            String mensagem,
            String mensagemErro,
            Exception ex) {

        Instant agora = Instant.now();

        log.warn(
                "Erro HTTP {}: {} - {}",
                status.value(),
                mensagem,
                mensagemErro,
                ex);

        ErroResponse<Void> erro = new ErroResponse<>();
        erro.setCodigo(status.value());
        erro.setMensagem(mensagem);
        erro.setInstante(agora);
        erro.setMensagemErro(mensagemErro);
        erro.setInstanteErro(agora);

        return ResponseEntity
                .status(status)
                .body(erro);
    }

    private String formatarErroCampo(FieldError erro) {
        return erro.getField() + ": " + erro.getDefaultMessage();
    }
}