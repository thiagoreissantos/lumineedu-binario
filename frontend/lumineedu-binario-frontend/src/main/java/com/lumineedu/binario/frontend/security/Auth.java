package com.lumineedu.binario.frontend.security;

import java.util.UUID;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

/**
 * Gerencia o token de autenticacao da interface.
 * O token fica associado a sessao Vaadin de cada usuario.
 */
@Component
public class Auth {

    public static final String COOKIE_NAME = "LUMINEEdu_SESSION";
    private static final String SESSION_TOKEN_KEY = Auth.class.getName() + ".token";
    private static final int EXPIRACAO_SEGUNDOS = 24 * 60 * 60;

    public String gerarToken() {
        String token = UUID.randomUUID().toString();
        setToken(token);
        return token;
    }

    public String getToken() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            return null;
        }

        String token = (String) session.getAttribute(SESSION_TOKEN_KEY);

        if (token == null) {
            token = lerCookie();
            if (token != null && !token.isBlank()) {
                session.setAttribute(SESSION_TOKEN_KEY, token);
            }
        }

        return token;
    }

    public boolean estaAutenticado() {
        String token = getToken();
        return token != null && !token.isBlank();
    }

    public void setToken(String token) {
        VaadinSession session = VaadinSession.getCurrent();

        if (session == null) {
            throw new IllegalStateException("Nao existe sessao Vaadin ativa.");
        }

        session.setAttribute(SESSION_TOKEN_KEY, token);
        escreverCookie(token, EXPIRACAO_SEGUNDOS);
    }

    public void desautenticar() {
        VaadinSession session = VaadinSession.getCurrent();

        if (session != null) {
            session.setAttribute(SESSION_TOKEN_KEY, null);
        }

        escreverCookie("", 0);
    }

    private String lerCookie() {
        VaadinRequest request = VaadinService.getCurrentRequest();

        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void escreverCookie(String token, int maxAge) {
        VaadinResponse response = VaadinService.getCurrentResponse();

        if (!(response instanceof HttpServletResponse)) {
            throw new IllegalStateException("Nao existe resposta HTTP ativa.");
        }

        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(
                VaadinService.getCurrentRequest() != null
                && VaadinService.getCurrentRequest().isSecure());

        ((HttpServletResponse) response).addCookie(cookie);
    }
}
