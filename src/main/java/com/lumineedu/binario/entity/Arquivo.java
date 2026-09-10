package com.lumineedu.binario.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.lumineedu.binario.dto.ArquivoDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa os metadados de um arquivo binario.
 * O conteudo binario nao e armazenado nesta entidade; ele e gravado
 * fisicamente no sistema de arquivos em disco local.
 */
@Entity
@Table(name = "tb_arquivo")
@Getter
@Setter
@NoArgsConstructor
public class Arquivo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String nomeOriginal;

    @Column(nullable = false, length = 500)
    private String nomeArquivo;

    @Column(nullable = false, length = 1000)
    private String caminhoRelativo;

    @Column(nullable = false, length = 1000)
    private String caminhoFisico;

    @Column(nullable = false)
    private Long tamanhoBytes;

    @Column(nullable = false, length = 255)
    private String tipoMime;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private Instant dataCriacao;

    @Column(updatable = false)
    private Instant dataAtualizacao;

    /**
     * Copia os campos relevantes da solicitacao para a entidade.
     *
     * @param dto a solicitacao de criacao do arquivo
     */
    public void copiarDe(ArquivoDTO dto) {
        this.nomeOriginal = dto.getNomeOriginal();
        this.nomeArquivo = dto.getNomeOriginal();
        this.tipoMime = dto.getTipoMime();
        this.descricao = dto.getDescricao();
    }

    /**
     * Define o caminho fisico e o tamanho do arquivo no disco.
     *
     * @param caminhoRelativo caminho relativo no sistema de arquivos
     * @param caminhoFisico   caminho completo no sistema de arquivos
     * @param tamanhoBytes    tamanho em bytes
     */
    public void definirCaminhoEConteudo(String caminhoRelativo, String caminhoFisico, long tamanhoBytes) {
        this.caminhoRelativo = caminhoRelativo;
        this.caminhoFisico = caminhoFisico;
        this.tamanhoBytes = tamanhoBytes;
    }

    @PrePersist
    protected void prePersist() {
        Instant agora = Instant.now();
        this.dataCriacao = this.dataCriacao != null ? this.dataCriacao : agora;
        this.dataAtualizacao = agora;
    }

    @PreUpdate
    protected void preUpdate() {
        this.dataAtualizacao = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Arquivo arquivo = (Arquivo) o;
        return id != null && Objects.equals(id, arquivo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
