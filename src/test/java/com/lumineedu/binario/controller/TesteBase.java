package com.lumineedu.binario.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.env.Environment;

/**
 * Base de suporte para os testes de integracao, que cria um diretorio
 * temporario isolado e expoe o caminho configurado para o contexto de
 * teste.
 */
public class TesteBase {

    private static final Path DIRETORIO;

    static {
        Path caminho = Path.of(System.getProperty("java.io.tmpdir"),
                "lumineedu-binario-test-" + System.currentTimeMillis());
        try {
            Files.createDirectories(caminho);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel criar o diretorio de teste", e);
        }
        DIRETORIO = caminho;
    }

    private final Environment environment;

    public TesteBase(Environment environment) {
        this.environment = environment;
    }

    public static Path diretorio() {
        return DIRETORIO;
    }

    public Path diretorioCompleto() {
        return Path.of(environment.getProperty("app.storage.diretorio", DIRETORIO.toAbsolutePath().toString()));
    }
}
