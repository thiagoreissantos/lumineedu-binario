package com.lumineedu.binario.dto;

import java.util.Base64;

import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Objeto de transferencia de dados para a solicitacao de criacao de
 * um arquivo binario. O conteudo do arquivo e enviado codificado em
 * Base64 no campo {@code conteudoBase64}, acompanhado dos metadados
 * necessarios (nome, tipo mime e descricao).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArquivoDTO {

    private static final int TAMANHO_MAXIMO_BASE64 = 67_108_864; // 64 MB em Base64

    @NotBlank(message = "nomeOriginal deve ser informado")
    @Size(max = 500, message = "nomeOriginal nao pode exceder 500 caracteres")
    private String nomeOriginal;

    @NotBlank(message = "tipoMime deve ser informado")
    private String tipoMime;

    @Size(max = 1000, message = "descricao nao pode exceder 1000 caracteres")
    private String descricao;

    @NotNull(message = "conteudoBase64 deve ser informado")
    @Size(max = TAMANHO_MAXIMO_BASE64, message = "conteudoBase64 excede o tamanho maximo permitido")
    private String conteudoBase64;

    /**
     * Decodifica o conteudo Base64 e retorna os bytes brutos do arquivo.
     *
     * @return o conteudo binario decodificado
     */
    public byte[] getConteudo() {
        if (conteudoBase64 == null || conteudoBase64.isBlank()) {
            throw new ArquivoInvalidoException("conteudoBase64 nao pode ser vazio");
        }
        try {
            return Base64.getDecoder().decode(conteudoBase64);
        } catch (IllegalArgumentException e) {
            throw new ArquivoInvalidoException("conteudoBase64 invalido: " + e.getMessage());
        }
    }

    /**
     * Retorna o tamanho em bytes do conteudo decodificado.
     *
     * @return tamanho em bytes
     */
    public long getTamanhoBytes() {
        return getConteudo().length;
    }

    /**
     * Valida o DTO, garantindo metadados e um tamanho compativel com o
     * limite configurado.
     *
     * @param limiteBytes limite maximo de tamanho em bytes
     */
    public void validar(long limiteBytes) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new ArquivoInvalidoException("nomeOriginal nao pode ser vazio");
        }
        if (tipoMime == null || tipoMime.isBlank()) {
            throw new ArquivoInvalidoException("tipoMime nao pode ser vazio");
        }
        if (nomeOriginal.contains(java.io.File.separator)
                || nomeOriginal.contains("..") || nomeOriginal.trim().isEmpty()) {
            throw new ArquivoInvalidoException("nomeOriginal contem caminho nao permitido");
        }
        long tamanho = getTamanhoBytes();
        if (tamanho > limiteBytes) {
            throw new TamanhoInvalidoException(
                    "arquivo excede o limite maximo de " + limiteBytes + " bytes");
        }
    }
}
