package com.lumineedu.binario.exception;

/**
 * Lançada quando o tamanho do arquivo recebido ultrapassa o limite
 * configurado (max-tamanho-bytes) ou o limite de disco.
 */
public class TamanhoInvalidoException extends ExcecaoNegocioException {

    public TamanhoInvalidoException() {
        super("tamanho do arquivo excede o limite permitido");
    }

    public TamanhoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
