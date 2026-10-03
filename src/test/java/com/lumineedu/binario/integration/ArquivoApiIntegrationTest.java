package com.lumineedu.binario.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResponseExtractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.repository.ArquivoRepository;
import org.springframework.data.domain.Page;
import com.lumineedu.binario.service.ArquivoCleanerService;
import com.lumineedu.binario.service.ArquivoService;
import com.lumineedu.binario.testutil.ImagemTeste;

/**
 * Testes de integracao entre a API (controle) e o banco de dados, executados
 * contra o contexto completo da aplicacao (controller -> seguranca -> servico
 * -> repositorio -> PostgreSQL real -> sistema de arquivos real). Nenhum
 * componente e mockado.
 *
 * <p>As chamadas HTTP sao feitas com {@link RestTemplate} (sincrona, HTTP real),
 * nao com WebClient nem MockMvc. A troca do corpo da resposta e a leitura do
 * corpo e a leitura do corpo e feitas com {@link ResponseExtractor} (o unico
 * extrator tipado disponivel nesta versao do spring-web), que permite afirmar o
 * status HTTP e parsear o JSON dentro de {@code extractData(org.springframework.http.client.ClientHttpResponse)}.
 * Cada metodo limpa a tabela antes de correr: {@code ArquivoService.criar()} e
 * {@code @Transactional}, por isso os registros se acumulariam entre os metodos
 * e as assertivas que dependem do conteudo da tabela (carga util, contagem por
 * mime) deixariam de bater.
 *
 * <p>A classe NAO e anotada {@code @Transactional} propositalmente: as chamadas
 * HTTP correm em transacoes independentes e seriam descartadas com o rollback.
 */
@SpringBootTest(webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://postgres:5432/lumineedu_binario",
        "spring.datasource.username=lumineedu",
        "spring.datasource.password=lumineedu",
        "spring.jpa.properties.jdbc.url=jdbc:postgresql://postgres:5432/lumineedu_binario"
})
class ArquivoApiIntegrationTest extends IntegrationTestBase {

    @org.springframework.boot.test.web.server.LocalServerPort
    int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    ArquivoService arquivoService;

    @Autowired
    ArquivoCleanerService arquivoCleanerService;

    @Autowired
    ArquivoRepository arquivoRepository;

    private String base;

    // ------------------------------------------------------------------
    // Isolacao por teste
    // ------------------------------------------------------------------
    //
    // Limpa a tabela antes de cada metodo e aponta base para o servidor
    // embedded (RANDOM_PORT). As chamadas HTTP usam http://localhost:<port>.
    //
    @BeforeEach
    void limparTabelaAntesDeCadaTeste() {
        arquivoRepository.deleteAll();
        this.base = "http://localhost:" + port;
    }

    // ------------------------------------------------------------------
    // Criacao via API
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/arquivos cria um arquivo, grava no disco e responde 201")
    void criaArquivoPorPost() {
        ArquivoDTO dto = criarDto("foto-de-teste.png", "image/png");

        ArquivoResponse response = request(HttpMethod.POST, "/api/arquivos", bearerHeaders(), dto,
                ArquivoResponse.class, 201);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getNomeOriginal()).isEqualTo("foto-de-teste.png");
        assertThat(response.getTamanhoBytes()).isPositive();
        assertThat(response.getAtivo()).isTrue();
        assertArquivoFisicoExiste("foto-de-teste.png");
    }

    // ------------------------------------------------------------------
    // Validacao de entrada
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/arquivos rejeita conteudo Base64 invalido com 400")
    void rejeitaConteudoInvalido() {
        ArquivoDTO dto = criarDto("arquivo-invalido.txt", "text/plain");
        dto.setConteudoBase64("!!!isto-nao-e-base64!!!");

        String body = requestRaw(HttpMethod.POST, "/api/arquivos", bearerHeaders(), dto, 400);

        assertThat(jsonInt(body, "codigo")).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/arquivos rejeita tipo mime fora da lista permitida com 400")
    void rejeitaTipoMimeInvalido() {
        ArquivoDTO dto = new ArquivoDTO();
        dto.setNomeOriginal("video-teste.exe");
        dto.setTipoMime("application/octet-stream");
        dto.setDescricao("descricao de teste");
        dto.setConteudoBase64(java.util.Base64.getEncoder().encodeToString(ImagemTeste.gerarPng()));

        String body = requestRaw(HttpMethod.POST, "/api/arquivos", bearerHeaders(), dto, 400);

        assertThat(jsonInt(body, "codigo")).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/arquivos com nome contendo caminho reaceita com 400")
    void rejeitaNomeComCaminho() {
        ArquivoDTO dto = criarDto("caminho/proibido.png", "image/png");

        String body = requestRaw(HttpMethod.POST, "/api/arquivos", bearerHeaders(), dto, 400);

        assertThat(jsonInt(body, "codigo")).isEqualTo(400);
    }

    // ------------------------------------------------------------------
    // Seguranca
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET sem token de autenticacao responde 401 JSON")
    void requerToken() {
        String body = requestRaw(HttpMethod.GET, "/api/arquivos/1", new HttpHeaders(), null, 401);

        assertThat(jsonInt(body, "codigo")).isEqualTo(401);
        assertThat(body).contains("codigo");
    }

    @Test
    @DisplayName("GET com token invalido responde 401 JSON")
    void rejeitaTokenInvalido() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer token-invalido-nao-existe");
        String body = requestRaw(HttpMethod.GET, "/api/arquivos/1", headers, null, 401);

        assertThat(jsonInt(body, "codigo")).isEqualTo(401);
    }

    @Test
    @DisplayName("GET com cabecalho X-API-TOKEN valido autoriza a requisicao")
    void aceitaHeaderApiToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-TOKEN", TOKEN);
        request(HttpMethod.GET, "/api/arquivos/tipo-mime/image/png", headers, null, Object.class, 200);
    }

    // ------------------------------------------------------------------
    // Leitura (GET)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos/{id} retorna o arquivo e registra uma leitura")
    void consultaArquivoPorId() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-para-ler.png", "image/png"));

        ArquivoResponse response = request(HttpMethod.GET, "/api/arquivos/" + criado.getId(), bearerHeaders(), null,
                ArquivoResponse.class, 200);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(criado.getId());
        assertThat(response.getNomeOriginal()).isEqualTo("arquivo-para-ler.png");
        assertThat(response.getQuantidadeLeituras()).isEqualTo(1);
        assertThat(response.getUltimaLeitura()).isNotNull();

        Arquivo reloaded = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(reloaded.isAtivo()).isTrue();
        assertThat(reloaded.getQuantidadeLeituras()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("GET /api/arquivos/{id} com id inexistente responde 404")
    void consultaArquivoIdInexistenteRetorna404() {
        String body = requestRaw(HttpMethod.GET, "/api/arquivos/99999999", bearerHeaders(), null, 404);

        assertThat(jsonInt(body, "codigo")).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /api/arquivos/nome/{nome} localiza o arquivo por nome original")
    void consultaArquivoPorNome() {
        arquivoService.criar(criarDto("documento-unico-abc.png", "image/png"));

        ArquivoResponse response = request(HttpMethod.GET, "/api/arquivos/nome/documento-unico-abc.png",
                bearerHeaders(), null, ArquivoResponse.class, 200);

        assertThat(response).isNotNull();
        assertThat(response.getNomeOriginal()).isEqualTo("documento-unico-abc.png");
    }

    @Test
    @DisplayName("GET /api/arquivos/nome/{nome} com nome ausente responde 404")
    void consultaArquivoPorNomeAusenteRetorna404() {
        String body = requestRaw(HttpMethod.GET, "/api/arquivos/nome/nao-existe.png", bearerHeaders(), null, 404);

        assertThat(jsonInt(body, "codigo")).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /api/arquivos lista os arquivos com paginaacao")
    void listaArquivosPaginado() {
        for (int i = 0; i < 3; i++) {
            arquivoService.criar(criarDto("lista-" + i + ".png", "image/png"));
        }

        Page<ArquivoResponse> pagina = request(HttpMethod.GET, "/api/arquivos?page=0&pageSize=2", bearerHeaders(),
                null, Page.class, 200);

        assertThat(pagina).isNotNull();
        assertThat(pagina.getNumber()).isEqualTo(0);
        assertThat(pagina.getSize()).isEqualTo(2);
        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("GET /api/arquivos/tipo-mime/{tipo} lista os arquivos de um tipo mime")
    void listaArquivosPorTipoMime() {
        arquivoService.criar(criarDto("png-1.png", "image/png"));
        arquivoService.criar(criarDto("jpg-1.jpg", "image/jpeg"));

        List<ArquivoResponse> lista = requestList(HttpMethod.GET, "/api/arquivos/tipo-mime/image/png",
                bearerHeaders(), 200);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getTipoMime()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("GET /api/arquivos/estatisticas retorna o total de arquivos")
    void estatisticas() {
        arquivoService.criar(criarDto("estat-1.png", "image/png"));
        arquivoService.criar(criarDto("estat-2.png", "image/png"));

        String body = requestRaw(HttpMethod.GET, "/api/arquivos/estatisticas", bearerHeaders(), null, 200);

        assertThat(jsonInt(body, "codigo")).isEqualTo(200);
        assertThat(jsonInt(body, "cargaUtil")).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // Exclusao (DELETE)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/arquivos/{id} remove o arquivo fisico e os metadados")
    void removeArquivoPorId() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-a-remover.png", "image/png"));

        request(HttpMethod.DELETE, "/api/arquivos/" + criado.getId(), bearerHeaders(), null, Object.class, 200);

        assertArquivoFisicoRemovido("arquivo-a-remover.png");
        assertThat(arquivoRepository.existsById(criado.getId())).isFalse();

        request(HttpMethod.DELETE, "/api/arquivos/" + criado.getId(), bearerHeaders(), null, Object.class, 404);
    }

    @Test
    @DisplayName("DELETE /api/arquivos/{id} com id inexistente responde 404")
    void removeArquivoIdInexistenteRetorna404() {
        String body = requestRaw(HttpMethod.DELETE, "/api/arquivos/99999999", bearerHeaders(), null, 404);

        assertThat(jsonInt(body, "codigo")).isEqualTo(404);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida / limpeza
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Arquivo inacessivel e antigo e removido fisicamente e desativado pela limpeza")
    void cicloDeVidaComLimpeza() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-antigo.png", "image/png"));

        Arquivo entidade = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(entidade.isAtivo()).isTrue();
        assertThat(entidade.getUltimaLeitura()).isNull();

        arquivoCleanerService.executar();

        Arquivo aposLimpeza = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(aposLimpeza.isAtivo()).isFalse();
        assertThat(aposLimpeza.getDataDesativacao()).isNotNull();
        assertThat(aposLimpeza.getQuantidadeLeituras()).isZero();
        assertArquivoFisicoRemovido("arquivo-antigo.png");

        String body = requestRaw(HttpMethod.GET, "/api/arquivos/" + criado.getId(), bearerHeaders(), null, 404);
        assertThat(jsonInt(body, "codigo")).isEqualTo(404);
    }

    @Test
    @DisplayName("Arquivo acessado antes da limpeza nao e removido pelo Job")
    void acessoProtegeArquivoDaLimpeza() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-acessado.png", "image/png"));

        ArquivoResponse lido = arquivoService.buscarPorId(criado.getId());
        assertThat(lido.getQuantidadeLeituras()).isEqualTo(1);

        arquivoCleanerService.executar();

        assertThat(arquivoRepository.findById(criado.getId()).orElseThrow().isAtivo()).isTrue();
        request(HttpMethod.GET, "/api/arquivos/" + criado.getId(), bearerHeaders(), null, Object.class, 200);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Constrói os cabecalhos {@code Authorization: Bearer <token>} da
     * configuracao de homologacao.
     */
    private static HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, "Bearer " + TOKEN);
        return headers;
    }

    private static ArquivoDTO criarDto(String nomeOriginal, String tipoMime) {
        byte[] conteudo = ImagemTeste.gerarPng();
        ArquivoDTO dto = new ArquivoDTO();
        dto.setNomeOriginal(nomeOriginal);
        dto.setTipoMime(tipoMime);
        dto.setDescricao("descricao de teste");
        dto.setConteudoBase64(java.util.Base64.getEncoder().encodeToString(conteudo));
        return dto;
    }

    /**
     * Envia uma requisicao HTTP real (RestTemplate) e devolve a resposta tipada
     * no tipo {@code type}. Afirma {@code expectedStatus} e parseia o JSON dentro
     * de {@code extractData}. Quando {@code corpo} nao e {@code null}, o corpo e
     * serializado para JSON e enviado no corpo da requisicao.
     *
     * @param method     o metodo HTTP.
     * @param url        o caminho (sem prefixo de host).
     * @param headers    os cabecalhos da requisicao.
     * @param corpo      o corpo da requisicao, ou {@code null}.
     * @param type       o tipo Java do corpo da resposta.
     * @param expectedStatus o codigo HTTP esperado.
     * @return o corpo da resposta tipado.
     */
    <T> T request(HttpMethod method, String url, HttpHeaders headers, Object corpo,
            Class<T> type, int expectedStatus) {
        RequestEntity<Object> entity = new RequestEntity<>(corpo, headers, method, URI.create(base + url));
        try {
            ResponseEntity<T> response = this.restTemplate.exchange(entity, new ParameterizedTypeReference<T>() {});
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("Falha ao parsear resposta: " + e.getMessage(), e);
        }
    }

    /**
     * Envia uma requisicao HTTP real (RestTemplate) para um endpoint que responde
     * uma string e devolve o corpo como texto. Afirma {@code expectedStatus}.
     *
     * @param method     o metodo HTTP.
     * @param url        o caminho (sem prefixo de host).
     * @param headers    os cabecalhos da requisicao.
     * @param corpo      o corpo da requisicao, ou {@code null}.
     * @param expectedStatus o codigo HTTP esperado.
     * @return o corpo da resposta como texto UTF-8.
     */
    String requestRaw(HttpMethod method, String url, HttpHeaders headers, Object corpo,
            int expectedStatus) {
        // O execute(URI, method, requestCallback, responseExtractor) envia os cabecalhos por
        // meio do RequestCallback (que tem o ClientHttpRequest) e devolve o corpo para QUALQUER
        // status. No sucesso, o extractor afirma o codigo e devolve o corpo lido do stream.
        // Em erro, o ResponseErrorHandler ja lancou antes do extractor; capturo a excecao, a
        // desvio para RestClientResponseException e devolvo o corpo armazenado nela. Assim
        // requestRaw SEMPRE devolve o corpo (2xx ou erro) e nada escapa como excecao.
        RequestCallback requestCallback = requestEntity -> {
            if (headers != null) {
                for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                    requestEntity.getHeaders().addAll(e.getKey(), e.getValue());
                }
            }
        };
        ResponseExtractor<String> extractor = response -> {
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
            return response.getBody() != null
                    ? new String(readAll(response.getBody()), StandardCharsets.UTF_8)
                    : "";
        };
        try {
            return this.restTemplate.execute(base + url, method, requestCallback, extractor);
        } catch (RestClientException e) {
            RestClientResponseException ex = (RestClientResponseException) e;
            assertThat(ex.getStatusCode().value()).isEqualTo(expectedStatus);
            return ex.getResponseBodyAsString();
        }
    }

    /**
     * Envia uma requisicao HTTP real (RestTemplate) para um endpoint que responde
     * uma lista e devolve a resposta tipada em {@code List<T>}. Afirma
     * {@code expectedStatus}.
     *
     * @param method     o metodo HTTP.
     * @param url        o caminho (sem prefixo de host).
     * @param headers    os cabecalhos da requisicao.
     * @param expectedStatus o codigo HTTP esperado.
     * @return a lista tipada com os elementos da resposta.
     */
    <T> List<T> requestList(HttpMethod method, String url, HttpHeaders headers, int expectedStatus)
            {
        RequestEntity<Object> entity = new RequestEntity<>(null, headers, method, URI.create(base + url));
        try {
            ResponseEntity<List<T>> response = this.restTemplate.exchange(entity,
                    new ParameterizedTypeReference<List<T>>() {});
            assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("Falha ao parsear resposta: " + e.getMessage(), e);
        }
    }

    /**
     * Parse o corpo JSON bruto da resposta em um {@code Map<String, Object>}.
     * A classe json-smart 2.x removeu o atalho {@code String.jsonPath()}, por
     * isso parseamos com Jackson e acessamos os campos como {@code Map}.
     */
    Map<String, Object> jsonBody(String body) {
        try {
            return jsonMapper.readValue(new StringReader(body), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao parsear JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Converte um campo numérico de um corpo JSON em {@code int}.
     */
    int jsonInt(String body, String chave) {
        Map<String, Object> json = jsonBody(body);
        Object valor = json.get(chave);
        if (valor == null) {
            throw new AssertionError("Chave '" + chave + "' ausente no corpo: " + body);
        }
        return ((Number) valor).intValue();
    }

    /**
     * Le o stream do corpo de uma {@link org.springframework.http.client.ClientHttpResponse} ate o fim, devolvendo
     * os bytes lidos.
     */
    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = is.read(chunk)) != -1) {
                out.write(chunk, 0, read);
            }
            return out.toByteArray();
        }
    }
}
