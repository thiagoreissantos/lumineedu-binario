package com.lumineedu.binario.frontend.service.web;

import java.util.List;
import java.util.Map;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lumineedu.binario.frontend.dto.ArquivoDTO;
import com.lumineedu.binario.frontend.dto.ArquivoResponse;
import com.lumineedu.binario.frontend.dto.EstatisticasResponse;
import com.lumineedu.binario.frontend.exception.ApiException;
import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.exception.ApiExceptionConverter;
import com.lumineedu.binario.frontend.security.Auth;

/**
 * Camada de acesso a dados da interface (frontend).
 * <p>
 * Contem os metodos que se comunicam diretamente com o backend via {@link
 * WebClient}, aplicando o token de autenticacao e convertendo os erros de
 * resposta HTTP em {@link ApiException}. Esta camada nao depende do servlet
 * container e nao usa nenhuma chamada HTTP embutida nas Views.
 */
@Service
public class WebClientService {

    /** Header padrao que transporta o token de autenticacao (aceito pelo backend). */
    private static final String TOKEN_HEADER = "Authorization";

    /** Header alternativo que transporta o token de autenticacao (aceito pelo backend). */
    private static final String API_TOKEN_HEADER = "X-API-TOKEN";

    private final WebClient webClient;
    private final Auth auth;
    private final ApiExceptionConverter converter;
    private final ApiExceptionHandler exceptionHandler;
    private final ObjectMapper objectMapper;

    public WebClientService(WebClient webClient, Auth auth, ApiExceptionConverter converter,
            ApiExceptionHandler exceptionHandler) {
        this.webClient = webClient;
        this.auth = auth;
        this.converter = converter;
        this.exceptionHandler = exceptionHandler;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retorna uma copia do {@link WebClient} com o token de autenticacao da
     * interface ja configurado nos headers {@code Authorization} e {@code
     * X-API-TOKEN}.
     */
    private WebClient comAutorizacao() {
        return webClient.mutate()
                .defaultHeader(TOKEN_HEADER, auth.getToken())
                .defaultHeader(API_TOKEN_HEADER, auth.getToken())
                .build();
    }

    /**
     * Converte uma excecao do {@link WebClient} (resposta HTTP com codigo de
     * status nao 2xx) em uma {@link ApiException} usando o conversor de erros.
     */
    private <T> Mono<T> comTratamentoDeErro(T chain) {
        return Mono.just(chain).onErrorResume(WebClientResponseException.class, e -> {
            String corpo = e.getResponseBodyAsString();
            int status = e.getStatusCode().value();
            return (Mono<T>) Mono.error(converter.converter(status, corpo));
        });
    }


    /**
     * Inclui um arquivo binario no backend, codificando o conteudo em Base64
     * conforme o contrato do DTO.
     *
     * @param dto   DTO com os metadados do arquivo (nome, tipo, descricao).
     * @param base64 conteudo binario codificado em Base64.
     * @return o arquivo incluido (com identificador e metadados do backend).
     */
    public ArquivoResponse incluir(ArquivoDTO dto, String base64) {
        dto.setConteudoBase64(base64);
        return comAutorizacao().post()
                .uri("/api/arquivos")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(ArquivoResponse.class)
                .flatMap(chain -> this.comTratamentoDeErro(chain))
                .block();
    }

    /**
     * Lista os arquivos de um tipo MIME dado.
     *
     * @param tipoMime tipo MIME dos arquivos a listar (ex.: {@code image/png}).
     */
    public List<ArquivoResponse> listarPorTipoMime(String tipoMime) {
        return (List<ArquivoResponse>) comAutorizacao().get()
                .uri("/api/arquivos/tipo-mime/{tipoMime}", tipoMime)
                .retrieve()
                .bodyToMono(List.class)
                .flatMap(chain -> this.comTratamentoDeErro(chain))
                .block();
    }

    /**
     * Lista os arquivos de forma paginada, com filtro opcional por descricao.
     * <p>
     * Os parametros de paginacao {@code page} e {@code size} e o filtro
     * {@code descricao} sao enviados como parametros de consulta, seguindo o
     * contrato do controller {@code GET /api/arquivos}.
     *
     * @param pageable paginacao (pagina e tamanho).
     * @param descricao filtro opcional por descricao.
     */
    public Page<ArquivoResponse> listarPage(Pageable pageable, String descricao) {
        return (Page<ArquivoResponse>) comAutorizacao().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/arquivos")
                        .queryParam("descricao", descricao)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve()
                .bodyToMono(org.springframework.data.domain.Page.class)
                .flatMap(chain -> this.comTratamentoDeErro(chain))
                .block();
    }

    /**
     * Retorna um arquivo por identificador.
     *
     * @param id identificador do arquivo.
     */
    public ArquivoResponse consultar(Long id) {
        return comAutorizacao().get()
                .uri("/api/arquivos/{id}", id)
                .retrieve()
                .bodyToMono(ArquivoResponse.class)
                .flatMap(chain -> this.comTratamentoDeErro(chain))
                .block();
    }

    /**
     * Remove um arquivo por identificador.
     *
     * @param id identificador do arquivo a remover.
     */
    public ArquivoResponse remover(Long id) {
        return comAutorizacao().delete()
                .uri("/api/arquivos/{id}")
                .retrieve()
                .bodyToMono(ArquivoResponse.class)
                .flatMap(chain -> this.comTratamentoDeErro(chain))
                .block();
    }

    /**
     * Obtém as estatisticas de armazenamento do backend.
     * <p>
     * O endpoint retorna um {@code GenericResponse} contendo {@code codigo},
     * {@code mensagem}, {@code instante}, {@code cargaUtil} (quantidade total
     * de arquivos) e {@code tamanhoTotalBytes}. O resultado e mapeado para o
     * {@link EstatisticasResponse} da interface.
     */
    public EstatisticasResponse estatisticas() {
        try {
            String corpo = comAutorizacao().get()
                    .uri("/api/arquivos/estatisticas")
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(chain -> this.comTratamentoDeErro(chain))
                    .block();

            Map<String, Object> dados;
            try {
                dados = objectMapper.readValue(
                        corpo, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException excecao) {
                throw new ApiException(-1, excecao.getMessage());
            }

            EstatisticasResponse resposta = new EstatisticasResponse();
            Object codigo = dados.get("codigo");
            resposta.setCodigo(codigo == null ? 0 : ((Number) codigo).intValue());

            Object mensagem = dados.get("mensagem");
            resposta.setMensagem(mensagem == null ? "" : mensagem.toString());

            Object instante = dados.get("instante");
            if (instante instanceof String) {
                resposta.setInstante(Instant.parse((String) instante));
            } else if (instante instanceof Number) {
                resposta.setInstante(Instant.ofEpochMilli(((Number) instante).longValue()));
            }

            Object carga = dados.get("cargaUtil");
            resposta.setCargaUtil(carga == null ? null : ((Number) carga).longValue());

            Object tamanhoTotal = dados.get("tamanhoTotalBytes");
            resposta.setTamanhoTotalBytes(tamanhoTotal == null ? null : ((Number) tamanhoTotal).longValue());

            return resposta;
        } catch (ApiException excecao) {
            throw new RuntimeException(exceptionHandler.amigavel(excecao));
        }
    }
}
