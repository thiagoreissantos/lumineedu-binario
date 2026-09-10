package com.lumineedu.binario.exception;

/**
 * Lançada quando ocorre um erro ao gravar ou recuperar o arquivo
 * no disco local (permissao negada, disco cheio, caminho invalido, etc).
 */
public class ArmazenamentoException extends ExcecaoNegocioException {

    public ArmazenamentoException() {
        super("erro ao armazenar o arquivo no disco");
    }

    public ArmazenamentoException(String mensagem) {
        super(mensagem);
    }
}
