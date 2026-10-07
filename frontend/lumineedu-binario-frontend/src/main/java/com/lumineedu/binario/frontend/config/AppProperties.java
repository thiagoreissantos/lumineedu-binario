package com.lumineedu.binario.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuracao da interface, lidas de {@code application.properties}.
 * <p>
 * Nenhuma credencial ou URL e hardcoded: todas as informacoes sensiveis (URL do
 * backend, usuario e senha de acesso) vem do ambiente de execucao.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** URL base da API REST do backend. */
    private String backendUrl = "http://localhost:8080";

    /** Credenciais de acesso da interface (configuraveis por perfil). */
    private final Security security = new Security();

    /** Nome exibido na interface. */
    private String uiName = "LumineEdu Binário";

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public Security getSecurity() {
        return security;
    }

    public String getUiName() {
        return uiName;
    }

    public void setUiName(String uiName) {
        this.uiName = uiName;
    }

    /**
     * Credenciais de acesso da interface, configuraveis por perfil.
     * <p>
     * O backend de autenticacao (token) e separado: a interface apenas valida
     * estas credenciais e, apos o login, gera um token que o backend aceita em
     * seus endpoints.
     */
    public static class Security {
        private String username = "admin";
        private String password = "alterar-esta-senha";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
