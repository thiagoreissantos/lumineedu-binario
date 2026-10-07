package com.lumineedu.binario.frontend.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Resposta das estatisticas do armazenamento.
 * <p>
 * Mapeia o {@code GenericResponse} retornado pelo endpoint de estatisticas do
 * backend. Os campos sao apenas os que o backend disponibiliza, sem inventar
 * nenhuma metrica.
 */
@Data
public class EstatisticasResponse {

    /** Codigo de status HTTP da resposta. */
    private int codigo;

    /** Mensagem descritiva da resposta. */
    private String mensagem;

    /** Instante em que a resposta foi gerada. */
    private Instant instante;

    /** Quantidade total de arquivos armazenados (carga util). */
    private Long cargaUtil;

    /** Soma do tamanho (em bytes) dos arquivos ativos. */
    private Long tamanhoTotalBytes;

    public int getCodigo() {
        return codigo;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Instant getInstante() {
        return instante;
    }

    public void setInstante(Instant instante) {
        this.instante = instante;
    }

    public Long getCargaUtil() {
        return cargaUtil;
    }

    public void setCargaUtil(Long cargaUtil) {
        this.cargaUtil = cargaUtil;
    }

    public Long getTamanhoTotalBytes() {
        return tamanhoTotalBytes;
    }

    public void setTamanhoTotalBytes(Long tamanhoTotalBytes) {
        this.tamanhoTotalBytes = tamanhoTotalBytes;
    }
}
