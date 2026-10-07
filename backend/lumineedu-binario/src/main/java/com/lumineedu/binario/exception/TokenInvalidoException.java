package com.lumineedu.binario.exception;

/**
 * Lançada quando o token de autenticacao fornecido na requisição
 * nao corresponde a nenhum token valido configurado.
 */
public class TokenInvalidoException extends ExcecaoNegocioException {

    public TokenInvalidoException() {
        super("token de autenticacao invalido");
    }

    public TokenInvalidoException(String mensagem) {
        super(mensagem);
    }
}
