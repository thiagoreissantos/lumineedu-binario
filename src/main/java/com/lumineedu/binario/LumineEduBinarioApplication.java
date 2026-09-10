package com.lumineedu.binario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe de entrada da aplicacao lumineedu-binario.
 * Responsavel por iniciar o contexto de aplicacao Spring Boot.
 */
@SpringBootApplication
public class LumineEduBinarioApplication {

    /**
     * Metodo principal de inicializacao da aplicacao.
     *
     * @param args argumentos de linha de comando (na sao utilizados)
     */
    public static void main(String[] args) {
        SpringApplication.run(LumineEduBinarioApplication.class, args);
    }
}
