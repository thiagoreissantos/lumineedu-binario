package com.lumineedu.binario.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.lumineedu.binario.entity.Arquivo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acesso a persistencia dos metadados dos arquivos.
 */
@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    /**
     * Busca todos os arquivos com um determinado nome original.
     *
     * @param nomeOriginal nome original do arquivo
     * @return a lista de arquivos encontrados
     */
    List<Arquivo> findByNomeOriginal(String nomeOriginal);

    /**
     * Busca todos os arquivos por tipo mime.
     *
     * @param tipoMime tipo mime do arquivo
     * @return a lista de arquivos encontrados
     */
    List<Arquivo> findByTipoMime(String tipoMime);

    /**
     * Busca uma pagina de arquivos.
     *
     * @param descricao filtro opcional sobre a descricao
     * @param pageable  paginacao
     * @return a pagina de arquivos
     */
    Page<Arquivo> findByDescricaoContainingIgnoreCase(String descricao, Pageable pageable);

    /**
     * Verifica se ja existe um arquivo com o mesmo nome original.
     *
     * @param nomeOriginal nome original a ser verificado
     * @return true se o arquivo ja existe
     */
    boolean existsByNomeOriginal(String nomeOriginal);

    /**
     * Contabiliza a quantidade total de arquivos.
     *
     * @return o total de arquivos
     */
    long count();

    /**
     * Retorna os arquivos candidatos a limpeza: registros ainda ativos e
     * que nao foram acessados (recuperados) dentro do periodo configuravel.
     * <p>
     * Inclui arquivos jamais lidos (ultima leitura ausente) e arquivos com
     * ultima leitura mais antiga que o instante limite. Filtrar por
     * {@code ativo = true} garante que a execucao do Job seja idempotente: um
     * arquivo ja removido (inativo) nao e um candidato a reprocessamento.
     *
     * @param cutoff limite temporal de acesso; arquivos com acesso anterior a
     *               este instante (ou sem acesso) sao considerados candidatos
     * @return a lista de arquivos candidatos a limpeza
     */
    @Query("SELECT a FROM Arquivo a WHERE a.ativo = true " +
           "AND (a.ultimaLeitura IS NULL OR a.ultimaLeitura < :cutoff)")
    List<Arquivo> encontrarCandidatosAPesquisa(Instant cutoff);
}
