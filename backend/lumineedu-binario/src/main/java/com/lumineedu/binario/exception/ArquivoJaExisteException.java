package com.lumineedu.binario.exception;

/**
 * Lançada quando um arquivo com o mesmo nome ja existe no sistema.
 */
public class ArquivoJaExisteException extends ExcecaoNegocioException {

    public ArquivoJaExisteException(String nomeOriginal) {
        super("arquivo '" + nomeOriginal + "' ja existe no sistema");
    }
}
