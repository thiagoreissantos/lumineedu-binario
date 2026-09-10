package com.lumineedu.binario.exception;

/**
 * Lançada quando o arquivo ou seus metadados sao invalidos
 * (nome vazio, tipo mime nao permitido, conteudo nao é imagem, etc).
 */
public class ArquivoInvalidoException extends ExcecaoNegocioException {

    public ArquivoInvalidoException() {
        super("arquivo invalido");
    }

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
