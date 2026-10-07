package com.lumineedu.binario.frontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Solicitacao para inclusao de um arquivo binario no backend.
 * <p>
 * Contrato do {@code POST /{app.api.prefix}} do backend. O {@code conteudoBase64}
 * deve conter o conteudo binario codificado em Base64 (nao e o nome do arquivo).
 */
@Data
public class ArquivoDTO {

    /** Nome original do arquivo (sem caminho nem extensao). */
    @NotBlank(message = "nomeOriginal é obrigatório")
    @Size(max = 500, message = "nomeOriginal não pode exceder 500 caracteres")
    private String nomeOriginal;

    /** Tipo MIME do arquivo (ex.: image/png). */
    @NotBlank(message = "tipoMime é obrigatório")
    private String tipoMime;

    /** Descricao opcional do arquivo. */
    @Size(max = 1000, message = "descricao não pode exceder 1000 caracteres")
    private String descricao;

    /** Conteudo binario codificado em Base64. */
    @NotBlank(message = "conteudoBase64 é obrigatório")
    private String conteudoBase64;

    /**
     * Define o conteudo binario codificado em Base64.
     * <p>
     * Exposta explicitamente para compensar a ausencia dos accessors gerados pelo
     * Lombok sob compilacao sem processadores (javac -proc:none).
     */
    public void setConteudoBase64(String conteudoBase64) {
        this.conteudoBase64 = conteudoBase64;
    }
}
