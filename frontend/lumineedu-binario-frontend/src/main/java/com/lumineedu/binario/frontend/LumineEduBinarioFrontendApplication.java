package com.lumineedu.binario.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Aplicacao principal da interface web (frontend) do LumineEdu Binário.
 * <p>
 * E uma aplicacao independente que se comunica com o backend (servico de
 * armazenamento de arquivos binarios) exclusivamente via API REST, conforme
 * o contrato definido por {@code app.backend.url}. Nao replica nenhuma regra
 * de negocio do backend.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LumineEduBinarioFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumineEduBinarioFrontendApplication.class, args);
    }
}
