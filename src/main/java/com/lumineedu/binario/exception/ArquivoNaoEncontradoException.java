package com.lumineedu.binario.exception;

/**
 * Lançada quando o arquivo solicitado nao existe no sistema.
 */
public class ArquivoNaoEncontradoException extends ExcecaoNegocioException {

    public ArquivoNaoEncontradoException() {
        super("arquivo nao encontrado");
    }
}
