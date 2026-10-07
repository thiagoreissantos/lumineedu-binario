package com.lumineedu.binario.exception;

/**
 * Classe base para todas as excecoes de negocio do sistema.
 * Nao e uma excecao do JDK, portanto nao interrompe o fluxo da aplicacao
 * de forma inesperada; e tratada globalmente pelo controller advice.
 */
public class ExcecaoNegocioException extends RuntimeException {

    public ExcecaoNegocioException(String mensagem) {
        super(mensagem);
    }
}
