package com.lumineedu.binario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.repository.ArquivoRepository;
import com.lumineedu.binario.service.ArquivoCleanerService;
import com.lumineedu.binario.service.ArquivoService;
import com.lumineedu.binario.testutil.ImagemTeste;

/**
 * Testes de integracao entre a API (controle) e o banco de dados, executados
 * contra o contexto completo da aplicacao (controller -> seguranca -> servico
 * -> repositorio -> PostgreSQL real -> sistema de arquivos real). Nenhum
 * componente e mockado.
 */
@SpringBootTest(webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ArquivoApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ArquivoService arquivoService;

    @Autowired
    ArquivoCleanerService arquivoCleanerService;

    @Autowired
    ArquivoRepository arquivoRepository;

    // ------------------------------------------------------------------
    // Isolacao por teste
    // ------------------------------------------------------------------
    //
    // ArquivoService.criar() e @Transactional e grava (commite) na tabela real.
    // Sem isso, os registros se acumulam entre os metodos e as assertivas que
    // dependem do conteudo da tabela (carga util, contagem por mime) deixariam
    // de bater. Limpa a tabela antes de cada metodo.
    //
    // A classe NAO e anotada @Transactional propositalmente: as chamadas HTTP
    // correm em transacoes independentes e seriam descartadas com o rollback.

    @BeforeEach
    void limparTabelaAntesDeCadaTeste() {
        arquivoRepository.deleteAll();
    }

    // ------------------------------------------------------------------
    // Criacao via API
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/arquivos cria um arquivo, grava no disco e responde 201")
    void criaArquivoPorPost() {
        ArquivoDTO dto = criarDto("foto-de-teste.png", "image/png");

        webTestClient.post().uri("/api/arquivos")
                .header(headerBearer())
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ArquivoResponse.class)
                .consumeWith(response -> {
                    ArquivoResponse criado = response.getResponseBody();
                    assertThat(criado).isNotNull();
                    assertThat(criado.getId()).isNotNull();
                    assertThat(criado.getNomeOriginal()).isEqualTo("foto-de-teste.png");
                    assertThat(criado.getTamanhoBytes()).isPositive();
                    assertThat(criado.getAtivo()).isTrue();
                    // Arquivo fisico gravado no diretorio isolado de teste.
                    assertArquivoFisicoExiste("foto-de-teste.png");
                });
    }

    @Test
    @DisplayName("POST /api/arquivos rejeita conteudo Base64 invalido com 400")
    void rejeitaConteudoInvalido() {
        ArquivoDTO dto = criarDto("arquivo-invalido.txt", "text/plain");
        dto.setConteudoBase64("!!!isto-nao-e-base64!!!");

        webTestClient.post().uri("/api/arquivos")
                .header(headerBearer())
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/arquivos rejeita tipo mime fora da lista permitida com 400")
    void rejeitaTipoMimeInvalido() {
        // A imagem e um PNG valido (bytes corretos), mas o tipo mime informado
        // nao pertence a lista permitida. A rejeicao vem do mime, nao da imagem.
        ArquivoDTO dto = new ArquivoDTO();
        dto.setNomeOriginal("video-teste.exe");
        dto.setTipoMime("application/octet-stream");
        dto.setDescricao("descricao de teste");
        byte[] conteudo = ImagemTeste.gerarPng();
        dto.setConteudoBase64(java.util.Base64.getEncoder().encodeToString(conteudo));

        webTestClient.post().uri("/api/arquivos")
                .header(headerBearer())
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(400);
    }

    @Test
    @DisplayName("POST /api/arquivos com nome contendo caminho eaceita com 400")
    void rejeitaNomeComCaminho() {
        ArquivoDTO dto = criarDto("caminho/proibido.png", "image/png");

        webTestClient.post().uri("/api/arquivos")
                .header(headerBearer())
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(400);
    }

    // ------------------------------------------------------------------
    // Autenticacao / seguranca
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET sem token de autenticacao responde 401 JSON")
    void requerToken() {
        webTestClient.get().uri("/api/arquivos/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(401)
                .consumeWith(response -> {
                    byte[] body = response.getResponseBody();
                    String corpo = new String(body, java.nio.charset.StandardCharsets.UTF_8);
                    assertThat(corpo).contains("codigo");
                });
    }

    @Test
    @DisplayName("GET com token invalido responde 401 JSON")
    void rejeitaTokenInvalido() {
        webTestClient.get().uri("/api/arquivos/1")
                .header("Authorization", "Bearer token-invalido-nao-existe")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(401);
    }

    @Test
    @DisplayName("GET com cabecalho X-API-TOKEN valido autoriza a requisicao")
    void aceitaHeaderApiToken() {
        webTestClient.get().uri("/api/arquivos/tipo-mime/image/png")
                .header("X-API-TOKEN", TOKEN)
                .exchange()
                .expectStatus().isOk();
    }

    // ------------------------------------------------------------------
    // Consulta por id
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos/{id} retorna o arquivo e registra uma leitura")
    void consultaArquivoPorId() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-para-ler.png", "image/png"));

        webTestClient.get().uri("/api/arquivos/" + criado.getId())
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(criado.getId())
                .jsonPath("$.nomeOriginal").isEqualTo("arquivo-para-ler.png")
                .jsonPath("$.quantidadeLeituras").isEqualTo(1)
                .jsonPath("$.ultimaLeitura").exists();

        Arquivo reloaded = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(reloaded.isAtivo()).isTrue();
        assertThat(reloaded.getQuantidadeLeituras()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("GET /api/arquivos/{id} com id inexistente responde 404")
    void consultaArquivoIdInexistenteRetorna404() {
        webTestClient.get().uri("/api/arquivos/99999999")
                .header(headerBearer())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(404);
    }

    // ------------------------------------------------------------------
    // Consulta por nome
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos/nome/{nome} localiza o arquivo por nome original")
    void consultaArquivoPorNome() {
        arquivoService.criar(criarDto("documento-unico-abc.png", "image/png"));

        webTestClient.get().uri("/api/arquivos/nome/documento-unico-abc.png")
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nomeOriginal").isEqualTo("documento-unico-abc.png");
    }

    @Test
    @DisplayName("GET /api/arquivos/nome/{nome} com nome ausente responde 404")
    void consultaArquivoPorNomeAusenteRetorna404() {
        webTestClient.get().uri("/api/arquivos/nome/nao-existe.png")
                .header(headerBearer())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(404);
    }

    // ------------------------------------------------------------------
    // Listagem paginada
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos lista os arquivos com paginaacao")
    void listaArquivosPaginado() {
        for (int i = 0; i < 3; i++) {
            arquivoService.criar(criarDto("lista-" + i + ".png", "image/png"));
        }

        webTestClient.get().uri("/api/arquivos?page=0&pageSize=2")
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Page.class)
                .consumeWith(response -> {
                    Page<ArquivoResponse> pagina = response.getResponseBody();
                    assertThat(pagina).isNotNull();
                    assertThat(pagina.getNumber()).isEqualTo(0);
                    assertThat(pagina.getSize()).isEqualTo(2);
                    assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(3);
                });
    }

    // ------------------------------------------------------------------
    // Listagem por tipo mime
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos/tipo-mime/{tipo} lista os arquivos de um tipo mime")
    void listaArquivosPorTipoMime() {
        arquivoService.criar(criarDto("png-1.png", "image/png"));
        arquivoService.criar(criarDto("jpg-1.jpg", "image/jpeg"));

        webTestClient.get().uri("/api/arquivos/tipo-mime/image/png")
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ArquivoResponse.class)
                .consumeWith(response -> {
                    List<ArquivoResponse> lista = response.getResponseBody();
                    assertThat(lista).hasSize(1);
                    assertThat(lista.get(0).getTipoMime()).isEqualTo("image/png");
                });
    }

    // ------------------------------------------------------------------
    // Estatisticas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/arquivos/estatisticas retorna o total de arquivos")
    void estatisticas() {
        arquivoService.criar(criarDto("estat-1.png", "image/png"));
        arquivoService.criar(criarDto("estat-2.png", "image/png"));

        webTestClient.get().uri("/api/arquivos/estatisticas")
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(200)
                .jsonPath("$.cargaUtil").isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // Remocao
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/arquivos/{id} remove o arquivo fisico e os metadados")
    void removeArquivoPorId() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-a-remover.png", "image/png"));

        webTestClient.delete().uri("/api/arquivos/" + criado.getId())
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk();

        // Arquivo fisico removido do sistema de arquivos.
        assertArquivoFisicoRemovido("arquivo-a-remover.png");

        // Metadados removidos do banco de dados.
        assertThat(arquivoRepository.existsById(criado.getId())).isFalse();

        // Tenta a remocao novamente e responde 404.
        webTestClient.delete().uri("/api/arquivos/" + criado.getId())
                .header(headerBearer())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /api/arquivos/{id} com id inexistente responde 404")
    void removeArquivoIdInexistenteRetorna404() {
        webTestClient.delete().uri("/api/arquivos/99999999")
                .header(headerBearer())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(404);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida (soft-delete via limpeza)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Arquivo inacessivel e antigo e removido fisicamente e desativado pela limpeza")
    void cicloDeVidaComLimpeza() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-antigo.png", "image/png"));

        // Simula um arquivo que nao foi acessado (sem ultima leitura).
        Arquivo entidade = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(entidade.isAtivo()).isTrue();
        assertThat(entidade.getUltimaLeitura()).isNull();

        // Executa o Job de limpeza diretamente (sem mock).
        arquivoCleanerService.executar();

        // Arquivo desativado e desativacao preenchida.
        Arquivo aposLimpeza = arquivoRepository.findById(criado.getId()).orElseThrow();
        assertThat(aposLimpeza.isAtivo()).isFalse();
        assertThat(aposLimpeza.getDataDesativacao()).isNotNull();
        assertThat(aposLimpeza.getQuantidadeLeituras()).isZero();

        // Arquivo fisico removido do disco.
        assertArquivoFisicoRemovido("arquivo-antigo.png");

        // Acesso por id agora responde 404 (inativo tratado como nao encontrado).
        webTestClient.get().uri("/api/arquivos/" + criado.getId())
                .header(headerBearer())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(404);
    }

    @Test
    @DisplayName("Arquivo acessado antes da limpeza nao e removido pelo Job")
    void acessoProtegeArquivoDaLimpeza() {
        ArquivoResponse criado = arquivoService.criar(criarDto("arquivo-acessado.png", "image/png"));

        // Acesso antes da limpeza atualiza a leitura.
        ArquivoResponse lido = arquivoService.buscarPorId(criado.getId());
        assertThat(lido.getQuantidadeLeituras()).isEqualTo(1);

        arquivoCleanerService.executar();

        // Arquivo preservado: ainda ativo e acessivel.
        assertThat(arquivoRepository.findById(criado.getId()).orElseThrow().isAtivo()).isTrue();
        webTestClient.get().uri("/api/arquivos/" + criado.getId())
                .header(headerBearer())
                .exchange()
                .expectStatus().isOk();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static ArquivoDTO criarDto(String nomeOriginal, String tipoMime) {
        byte[] conteudo = ImagemTeste.gerarPng();
        ArquivoDTO dto = new ArquivoDTO();
        dto.setNomeOriginal(nomeOriginal);
        dto.setTipoMime(tipoMime);
        dto.setDescricao("descricao de teste");
        dto.setConteudoBase64(java.util.Base64.getEncoder().encodeToString(conteudo));
        return dto;
    }
}
