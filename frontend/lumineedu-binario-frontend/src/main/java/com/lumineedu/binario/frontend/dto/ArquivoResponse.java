package com.lumineedu.binario.frontend.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Resposta de um arquivo retornado pelo backend.
 * <p>
 * Mapeia exatamente os campos do {@code ArquivoResponse} do backend, que servem
 * de coluna para a tabela de listagem e de informacao para as estatisticas.
 */
@Data
public class ArquivoResponse {

    private Long id;
    private String nomeOriginal;
    private String nomeArquivo;
    private String caminhoRelativo;
    private String caminhoFisico;
    private Long tamanhoBytes;
    private String tipoMime;
    private String descricao;
    private Instant dataCriacao;
    private Instant dataAtualizacao;
    private Instant ultimaLeitura;
    private Long quantidadeLeituras;
    private Boolean ativo;
    private Instant dataDesativacao;
    private String hash;
}
