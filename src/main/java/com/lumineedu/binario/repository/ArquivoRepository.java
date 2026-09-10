package com.lumineedu.binario.repository;

import java.util.List;
import java.util.Optional;

import com.lumineedu.binario.entity.Arquivo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
