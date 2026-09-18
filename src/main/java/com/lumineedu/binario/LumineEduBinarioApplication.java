package com.lumineedu.binario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe de entrada da aplicacao lumineedu-binario.
 * Responsavel por iniciar o contexto de aplicacao Spring Boot.
 */
@SpringBootApplication(
        exclude = {
            UserDetailsServiceAutoConfiguration.class
        }
)
@ConfigurationPropertiesScan
@EnableScheduling
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
