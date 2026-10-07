package com.lumineedu.binario.frontend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lumineedu.binario.frontend.dto.ArquivoDTO;
import com.lumineedu.binario.frontend.dto.ArquivoResponse;
import com.lumineedu.binario.frontend.dto.EstatisticasResponse;
import com.lumineedu.binario.frontend.service.web.WebClientService;

/**
 * Camada de negocio da interface (frontend) para gerenciar arquivos.
 * <p>
 * Encapsula a logica de negocio (montagem dos parametros, validacao de
 * retorno) e delega as chamadas HTTP para o {@link WebClientService}. As Views
 * apenas utilizam esta camada e nunca fazem chamadas HTTP diretamente.
 */
@Service
public class BinarioService {

    private final WebClientService webClientService;

    public BinarioService(WebClientService webClientService) {
        this.webClientService = webClientService;
    }

    /**
     * Inclui um arquivo binario no backend.
     *
     * @param dto       DTO com os metadados do arquivo (nome, tipo, descricao).
     * @param base64    conteudo binario codificado em Base64.
     * @return o arquivo incluido (com identificador e metadados do backend).
     */
    public ArquivoResponse incluir(ArquivoDTO dto, String base64) {
        dto.setConteudoBase64(base64);
        return webClientService.incluir(dto, base64);
    }

    /**
     * Lista os arquivos de um tipo MIME dado.
     *
     * @param tipoMime tipo MIME dos arquivos a listar.
     */
    public List<ArquivoResponse> listarPorTipoMime(String tipoMime) {
        return webClientService.listarPorTipoMime(tipoMime);
    }

    /**
     * Lista os arquivos de forma paginada, com filtro opcional por descricao.
     *
     * @param pageable  paginacao (pagina e tamanho).
     * @param descricao filtro opcional por descricao.
     */
    public Page<ArquivoResponse> listarPage(Pageable pageable, String descricao) {
        return webClientService.listarPage(pageable, descricao);
    }

    /**
     * Retorna um arquivo por identificador.
     *
     * @param id identificador do arquivo.
     */
    public ArquivoResponse consultar(Long id) {
        return webClientService.consultar(id);
    }

    /**
     * Remove um arquivo por identificador.
     *
     * @param id identificador do arquivo a remover.
     */
    public ArquivoResponse remover(Long id) {
        return webClientService.remover(id);
    }

    /**
     * Retorna as estatisticas de armazenamento do backend.
     *
     * @return as estatisticas de armazenamento.
     */
    public EstatisticasResponse estatisticas() {
        return webClientService.estatisticas();
    }
}
