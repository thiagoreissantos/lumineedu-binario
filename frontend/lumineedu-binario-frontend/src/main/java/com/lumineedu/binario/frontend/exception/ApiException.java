package com.lumineedu.binario.frontend.exception;

/**
 * Excecao de dominio que representa um erro de comunicacao com o backend.
 * <p>
 * Carrega o codigo HTTP e a mensagem bruta da resposta do backend, permitindo
 * que a camada de apresentacao apresente uma mensagem amigavel ao usuario sem
 * expor detalhes da stack trace.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String mensagem;

    public ApiException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
        this.mensagem = mensagem;
    }

    public int getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }
}
