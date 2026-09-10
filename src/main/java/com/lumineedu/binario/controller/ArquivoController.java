package com.lumineedu.binario.controller;

import java.time.Instant;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.dto.GenericResponse;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.exception.ErroResponse;
import com.lumineedu.binario.exception.handler.GlobalExceptionHandler;
import com.lumineedu.binario.service.ArquivoService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Controller de endpoints REST para administracao de arquivos binarios.
 * Todas as operacoes sao protegidas por token de autenticacao.
 */
@RestController
@RequestMapping("${app.api.prefix}")
@RequiredArgsConstructor
public class ArquivoController {

    private final ArquivoService arquivoService;
    private final GlobalExceptionHandler handler;

    /**
     * Cria um novo arquivo binario a partir do conteudo codificado em
     * Base64 e dos metadados fornecidos.
     *
     * @param dto a solicitacao de criacao
     * @return a resposta do arquivo criado
     */
    @PostMapping
    public ResponseEntity<ArquivoResponse> criar(@Valid @RequestBody ArquivoDTO dto) {
        ArquivoResponse resposta = arquivoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * Busca um arquivo por identificador.
     *
     * @param id identificador do arquivo
     * @return a resposta do arquivo
     */
    @GetMapping("/{id}")
    public ArquivoResponse consultar(@PathVariable Long id) {
        return arquivoService.buscarPorId(id);
    }

    /**
     * Busca um arquivo por nome original.
     *
     * @param nomeOriginal nome original do arquivo
     * @return a resposta do arquivo
     */
    @GetMapping("/nome/{nomeOriginal}")
    public ArquivoResponse consultarPorNome(@PathVariable String nomeOriginal) {
        return arquivoService.buscarPorNome(nomeOriginal);
    }

    /**
     * Lista os arquivos de um determinado tipo mime.
     *
     * @param tipoMime o tipo mime dos arquivos
     * @return a lista de respostas de arquivos
     */
    @GetMapping("/tipo-mime/{tipoMime}")
    public java.util.List<ArquivoResponse> listarPorTipoMime(@PathVariable String tipoMime) {
        return arquivoService.listarPorTipoMime(tipoMime);
    }

    /**
     * Listagem paginada de todos os arquivos.
     *
     * @param descricao filtro opcional sobre a descricao
     * @param pageable  a paginacao
     * @return a pagina de respostas de arquivos
     */
    @GetMapping
    public Page<ArquivoResponse> listar(@RequestParam(required = false) String descricao,
            Pageable pageable) {
        return arquivoService.listar(pageable);
    }

    /**
     * Retorna estatisticas gerais do armazenamento.
     *
     * @return a resposta com estatisticas
     */
    @GetMapping("/estatisticas")
    public GenericResponse<Long> estatisticas() {
        Instant instante = Instant.now();
        GenericResponse<Long> resposta = new GenericResponse<>(
                HttpStatus.OK.value(), "estatisticas obtidas", instante);
        Long total = arquivoService.contar();
        resposta.setCargaUtil(total);
        return resposta;
    }

    /**
     * Remove um arquivo do sistema.
     *
     * @param id identificador do arquivo
     * @return a resposta indicando sucesso da remocao
     */
    @DeleteMapping("/{id}")
    public ArquivoResponse remover(@PathVariable Long id) {
        return arquivoService.remover(id);
    }
}
