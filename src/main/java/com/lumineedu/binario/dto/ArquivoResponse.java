package com.lumineedu.binario.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.lumineedu.binario.entity.Arquivo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Objeto de transferencia de dados retornado para o cliente ao consultar
 * ou listar um arquivo. Contem apenas os metadados do arquivo, jamais o
 * conteudo binario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * Constrói uma resposta a partir de uma entidade do banco de dados.
     *
     * @param arquivo a entidade a ser transformada
     * @return a resposta formatada
     */
    public static ArquivoResponse de(Arquivo arquivo) {
        return ArquivoResponse.builder()
                .id(arquivo.getId())
                .nomeOriginal(arquivo.getNomeOriginal())
                .nomeArquivo(arquivo.getNomeArquivo())
                .caminhoRelativo(arquivo.getCaminhoRelativo())
                .caminhoFisico(arquivo.getCaminhoFisico())
                .tamanhoBytes(arquivo.getTamanhoBytes())
                .tipoMime(arquivo.getTipoMime())
                .descricao(arquivo.getDescricao())
                .dataCriacao(arquivo.getDataCriacao())
                .dataAtualizacao(arquivo.getDataAtualizacao())
                .ultimaLeitura(arquivo.getUltimaLeitura())
                .quantidadeLeituras(arquivo.getQuantidadeLeituras())
                .build();
    }

    /**
     * Converte a data de criacao da entidade para um formato legivel
     * em formato de timestamp (ISO-8601).
     *
     * @return a data de criacao formatada
     */
    public String formatarDataCriacao() {
        return dataCriacao != null
                ? dataCriacao.atZone(ZoneId.of("America/Sao_Paulo"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : null;
    }

    /**
     * Converte a data da ultima leitura da entidade para um formato legivel
     * em formato de timestamp (ISO-8601).
     *
     * @return a data da ultima leitura formatada, ou null se nenhuma leitura
     */
    public String formatarUltimaLeitura() {
        return ultimaLeitura != null
                ? ultimaLeitura.atZone(ZoneId.of("America/Sao_Paulo"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : null;
    }
}
