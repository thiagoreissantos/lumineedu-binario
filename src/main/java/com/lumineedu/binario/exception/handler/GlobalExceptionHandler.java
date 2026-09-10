package com.lumineedu.binario.exception.handler;

import java.time.Instant;

import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArquivoNaoEncontradoException;
import com.lumineedu.binario.exception.ArmazenamentoException;
import com.lumineedu.binario.exception.ExcecaoNegocioException;
import com.lumineedu.binario.exception.TokenInvalidoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;
import com.lumineedu.binario.exception.ErroResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Handler global de excecoes. Captura excecoes de negocio e de validacao
 * e retorna respostas REST padronizadas com o codigo HTTP apropriado.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata o token de autenticacao invalido ou ausente.
     *
     * @param ex a excecao de token invalido
     * @return a resposta de erro com status 401
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleTokenInvalido(TokenInvalidoException ex) {
        return construirResposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Trata arquivos invalidos (nome, tipo mime, conteudo, tamanho).
     *
     * @param ex a excecao de arquivo invalido
     * @return a resposta de erro com status 400
     */
    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoInvalido(ArquivoInvalidoException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Trata tamanho de arquivo invalido.
     *
     * @param ex a excecao de tamanho invalido
     * @return a resposta de erro com status 413
     */
    @ExceptionHandler(TamanhoInvalidoException.class)
    public ResponseEntity<ErroResponse<Void>> handleTamanhoInvalido(TamanhoInvalidoException ex) {
        return construirResposta(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    /**
     * Trata arquivo ja existente.
     *
     * @param ex a excecao de arquivo ja existe
     * @return a resposta de erro com status 409
     */
    @ExceptionHandler(ArquivoJaExisteException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoJaExiste(ArquivoJaExisteException ex) {
        return construirResposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Trata arquivo nao encontrado.
     *
     * @param ex a excecao de arquivo nao encontrado
     * @return a resposta de erro com status 404
     */
    @ExceptionHandler(ArquivoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArquivoNaoEncontrado(ArquivoNaoEncontradoException ex) {
        return construirResposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Trata erros de armazenamento no disco.
     *
     * @param ex a excecao de armazenamento
     * @return a resposta de erro com status 500
     */
    @ExceptionHandler(ArmazenamentoException.class)
    public ResponseEntity<ErroResponse<Void>> handleArmazenamento(ArmazenamentoException ex) {
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Trata excecoes de negocio genericas.
     *
     * @param ex a excecao de negocio
     * @return a resposta de erro com status 400
     */
    @ExceptionHandler(ExcecaoNegocioException.class)
    public ResponseEntity<ErroResponse<Void>> handleExcecaoNegocio(ExcecaoNegocioException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Trata excecoes de acesso negado.
     *
     * @param ex a excecao de acesso negado
     * @return a resposta de erro com status 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse<Void>> handleAcessoNegado(AccessDeniedException ex) {
        return construirResposta(HttpStatus.FORBIDDEN, "acesso nao autorizado");
    }

    /**
     * Trata falhas de validacao de campos (Bean Validation).
     *
     * @param ex a excecao de argumentos nao validos
     * @return a resposta de erro com status 400 e os campos invalidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse<Void>> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatarErroCampo)
                .collect(Collectors.joining(", "));
        return construirResposta(HttpStatus.BAD_REQUEST, mensagem);
    }

    /**
     * Trata excecoes nao tratadas.
     *
     * @param ex a excecao generica
     * @return a resposta de erro com status 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse<Void>> handleGeral(Exception ex) {
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "ocorreu um erro inesperado: " + ex.getMessage());
    }

    /**
     * Constrói uma resposta de erro padronizada.
     *
     * @param status     o status HTTP
     * @param mensagem   a mensagem de erro
     * @return a resposta de erro com o status configurado
     */
    private ResponseEntity<ErroResponse<Void>> construirResposta(HttpStatus status, String mensagem) {
        Instant instante = Instant.now();
        ErroResponse<Void> erro = new ErroResponse<>(status.value(), mensagem, instante);
        return ResponseEntity.status(status).body(erro);
    }

    /**
     * Formata a mensagem de um erro de campo de validacao.
     *
     * @param erro o erro de campo
     * @return a mensagem formatada
     */
    private String formatarErroCampo(FieldError erro) {
        return erro.getField() + ": " + erro.getDefaultMessage();
    }
}
