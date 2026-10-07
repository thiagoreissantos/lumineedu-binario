package com.lumineedu.binario.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configura o {@link WebClient} usado pela interface para se comunicar com o
 * backend.
 * <p>
 * A URL base vem de {@link AppProperties#getBackendUrl()} (nao e hardcoded),
 * conforme {@code app.backend.url} em {@code application.properties}. O tamanho
 * maximo da memoria para o corpo da solicitação e aumentado para suportar o
 * envio de conteudo binario codificado em Base64.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Tamanho maximo (em bytes) do corpo em memoria durante a serializacao/
     * deserializacao, aumentado para conteudo binario codificado em Base64.
     */
    private static final int MAX_IN_MEMORY_BYTES = 50 * 1024 * 1024;

    @Bean
    public WebClient webClient(AppProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBackendUrl())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();
    }
}
