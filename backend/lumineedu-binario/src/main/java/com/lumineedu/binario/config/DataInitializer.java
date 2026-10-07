package com.lumineedu.binario.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Carrega os dados iniciais de exemplo (metadados) apenas no ambiente
 * de desenvolvimento.
 *
 * <p>Em homol/prod o schema e gerido externamente e o {@code data.sql}
 * contem caminhos absolutos de desenvolvimento (ex.:
 * {@code /home/lumineedu/armazenamento/...}); por isso e limitado ao
 * perfil {@code dev} via {@link Profile}, evitando falhas de
 * inicializacao nos ambientes de producao.
 */
@Component
@Profile("dev")
public class DataInitializer {

    private final DataSource dataSource;
    private final ResourceDatabasePopulator populator;

    public DataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
        this.populator = new ResourceDatabasePopulator();
        this.populator.setContinueOnError(true);
        this.populator.addScript(new ClassPathResource("data.sql"));
    }

    /**
     * Executa o script {@code data.sql} do classpath.
     * <p>Usa {@link ResourceDatabasePopulator#setContinueOnError(boolean)}
     * para que um statement falho nao impeca a inicializacao do resto da
     * aplicacao (comportamento tolerante para dados de exemplo).
     */
    public void init() {
        this.populator.execute(this.dataSource);
    }
}
