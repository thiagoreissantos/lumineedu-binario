package com.lumineedu.binario.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Objeto de configuracao que mapeia as propriedades do bloco
 * {@code app.security}. Controla os tokens validos e os tipos mime
 * permitidos para upload de imagens.
 */
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** Tokens aceitos na cabecalho (Bearer-Token ou X-API-TOKEN). */
    private List<String> tokensValidos = new ArrayList<>();

    /** Tipos mime permitidos para upload de imagem. */
    private List<String> mimeTiposPermitidos = new ArrayList<>();

    public List<String> getTokensValidos() {
        return tokensValidos;
    }

    public void setTokensValidos(List<String> tokensValidos) {
        this.tokensValidos = tokensValidos;
    }

    public List<String> getMimeTiposPermitidos() {
        return mimeTiposPermitidos;
    }

    public void setMimeTypeosPermitidos(List<String> mimeTiposPermitidos) {
        this.mimeTiposPermitidos = mimeTiposPermitidos;
    }
}
