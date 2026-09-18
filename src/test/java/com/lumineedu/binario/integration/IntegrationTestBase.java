package com.lumineedu.binario.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;

/**
 * Base para os testes de integracao sem mocking.
 *
 * <p>Cada classe de teste que estender esta base:
 * <ul>
 *   <li>inicia o contexto completo da aplicacao (controller -> seguranca ->
 *       servico -> repositorio -> PostgreSQL real -> sistema de arquivos real);
 *   <li>isola o armazenamento fisico em um diretorio temporario unico por
 *       classe de teste, sobrescrevendo {@code app.storage.diretorio} antes da
 *       construcao do contexto.
 * </ul>
 */
abstract class IntegrationTestBase {

    /** Token valido da configuracao de homologacao. */
    protected static final String TOKEN = "token-secreto-lumine";

    private static Path diretorioDeTeste;

    @BeforeAll
    static void prepararDiretorioDeTeste() {
        diretorioDeTeste = Path.of(System.getProperty("java.io.tmpdir"),
                "lumineedu-binario-test-" + System.nanoTime());
        try {
            Files.createDirectories(diretorioDeTeste);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel criar o diretorio de teste: " + diretorioDeTeste, e);
        }
        // Sobrescreve app.storage.diretorio (perfil homol) antes da construcao do
        // contexto. O @DynamicPropertySource e ausente neste repositorio local,
        // entao usamos uma propriedade de sistema (prioridade mais alta que os
        // arquivos .properties do contexto de aplicacao) como substituto.
        System.setProperty("app.storage.diretorio", diretorioDeTeste.toString());
        // Garante permissoes de escrita (GNU/Linux).
        try {
            if (Files.exists(diretorioDeTeste) && Files.getFileStore(diretorioDeTeste).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> permissive = PosixFilePermissions.fromString("rwxrwxrwx");
                Files.setPosixFilePermissions(diretorioDeTeste, permissive);
            }
        } catch (UnsupportedOperationException | IOException ignored) {
            // Sistemas sem suporte POSIX (ex.: Windows) ignoram.
        }
    }

    protected static void limparDiretorioDeTeste() {
        if (diretorioDeTeste != null && Files.exists(diretorioDeTeste)) {
            try (var stream = Files.list(diretorioDeTeste)) {
                stream.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // Melhor esforco: remove o que for removivel.
                    }
                });
            } catch (IOException ignored) {
                // Ignorado: o diretorio sera removido pelo GC do contexto de teste.
            }
        }
    }

    /** Verifica que um arquivo fisico foi gravado no diretorio de teste. */
    protected static void assertArquivoFisicoExiste(String nomeArquivoRelativo) {
        // O arquivo e gravado em <diretorio>/<subdiretorio>/<nome>, onde o
        // subdiretorio por padrao (e o que o sistema usa em producao) e "arquivos".
        Path caminho = Path.of(diretorioDeTeste.toString(), "arquivos", nomeArquivoRelativo);
        assertThat(caminho).exists().isRegularFile();
    }

    /** Verifica que um arquivo fisico foi removido do diretorio de teste. */
    protected static void assertArquivoFisicoRemovido(String nomeArquivoRelativo) {
        // O arquivo e gravado em <diretorio>/<subdiretorio>/<nome>, onde o
        // subdiretorio por padrao (e o que o sistema usa em producao) e "arquivos".
        Path caminho = Path.of(diretorioDeTeste.toString(), "arquivos", nomeArquivoRelativo);
        assertThat(caminho).doesNotExist();
    }

    /** Verifica que o conteudo Base64 de um arquivo gravado e o esperado. */
    protected static void assertArquivoConteudoValido() {
        // Garantido que o diretorio de teste nao ficou vazio apos a gravacao.
        try {
            assertThat(Files.list(diretorioDeTeste)).hasSizeGreaterThanOrEqualTo(1);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel listar o diretorio de teste: " + diretorioDeTeste, e);
        }
    }

    /** Retorna o header de autenticacao {@code Authorization: Bearer <token>}. */
    protected static String headerBearer() {
        return "Authorization: Bearer " + TOKEN;
    }

    /**
     * Verifica que uma string e uma sequencia Base64 padrao (carregada como
     * texto, sem um arquivo especifico).
     */
    protected static String base64DeExemplo() {
        return java.util.Base64.getEncoder().encodeToString("integration-test".getBytes());
    }
}
