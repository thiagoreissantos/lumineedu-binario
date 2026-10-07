package com.lumineedu.binario.frontend.security;

import jakarta.servlet.http.Cookie;

import org.springframework.stereotype.Component;

/**
 * Gerencia o token de sessao da interface (autenticacao do frontend).
 * <p>
 * A autenticacao da interface e independente da do backend: a interface apenas
 * valida as credenciais do usuario e guarda um token que o backend aceita em
 * seus endpoints. Nao existe sessao/cookie compartilhado com o backend; o
 * logout e puramente uma acao do frontend (remove o token local).
 */
@Component
public class Auth {

    /** Nome do cookie que armazena o token de sessao da interface. */
    public static final String COOKIE_NAME = "LUMINEEdu_SESSION";

    /** Duracao da sessao em segundos (24 horas). */
    private static final long EXPIRACAO_SEGUNDOS = 24L * 60 * 60;

    private final Cookie cookie;
    private String token;

    public Auth(Cookie cookie) {
        this.cookie = cookie;
    }

    /**
     * Gera um token de sessao aleatorio, armazena-o localmente e escreve-o no
     * cookie da interface.
     *
     * @return o token gerado (nunca vazio nem sensivel)
     */
    public String gerarToken() {
        String token = java.util.UUID.randomUUID().toString();
        setToken(token);
        return token;
    }

    public String getToken() {
        if (token == null) {
            token = cookie != null && COOKIE_NAME.equals(cookie.getName()) ? cookie.getValue() : null;
        }
        return token;
    }

    public boolean estaAutenticado() {
        String t = getToken();
        return t != null && !t.isBlank();
    }

    /** Armazena o token no cookie da interface. */
    public void setToken(String token) {
        this.token = token;
        Cookie c = new Cookie(COOKIE_NAME, token);
        c.setPath("/");
        c.setMaxAge((int) EXPIRACAO_SEGUNDOS);
        c.setHttpOnly(true);
    }

    /** Remove o token da interface (logout). */
    public void desautenticar() {
        Cookie c = new Cookie(COOKIE_NAME, null);
        c.setPath("/");
        c.setMaxAge(0);
        c.setHttpOnly(true);
        this.token = null;
    }
}
