package com.lumineedu.binario.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import com.lumineedu.binario.frontend.views.ArquivosUploadServlet;

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

    /**
     * Registra o servlet bruto de upload de arquivos no container, com o mapeamento
     * "/arquivos/upload".
     * <p>
     * O servlet e' gerido pelo Spring como um @Component (injecao de construtor),
     * e' passado para o {@link ServletRegistrationBean}, que o registra no container
     * de servlets durante a inicializacao do contexto. Assim, sem depender do
     * {@code web.xml} nem de anotacoes de mapeamento de servlet, o servlet esta'
     * disponivel antes do {@code VaadinServlet}.
     */
    @Bean
    public ServletRegistrationBean<ArquivosUploadServlet> arquivosUploadServlet(ArquivosUploadServlet servlet) {
        return new ServletRegistrationBean<>(servlet, "/arquivos/upload");
    }
}
