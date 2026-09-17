package com.lumineedu.binario.security;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lumineedu.binario.config.SecurityProperties;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Configuracao do filtro de autenticacao por token fixo.
 *
 * <p>Em Spring Security 6 a classe servlet {@code FilterOncePerRequest} foi
 * removida, portanto o filtro de token e registrado como um filtro de
 * servlet padrao ({@link Filter}) através de um {@link FilterRegistrationBean}.
 * Executado com a maior precedencia, roda antes da cadeia de seguranca do
 * Spring: quando o token e valido, autentica o contexto de seguranca para
 * que a cadeia autorize a requisicao; quando ausente ou invalido, responde
 * 401 antes de chegar a qualquer controller.</p>
 */
@Configuration
@RequiredArgsConstructor
public class TokenFilterConfig {

    private final SecurityProperties properties;
    
    @Bean
    public TokenFilter tokenFilter() {
        return new TokenFilter(properties);
    }

    /**
     * Filtro de autenticacao por token fixo.
     *
     * <p>Classe interna static instantiada pelo Spring como bean com injecao
     * de dependencia do {@link SecurityProperties}. Implementa {@link Filter}
     * e {@link Ordered}: o {@link #doFilter(ServletRequest, ServletResponse,
     * FilterChain)} e o metodo abstrato da interface {@link Filter}. As
     * requisi/respostas HTTP sao obtidas por casting seguro (todo
     * {@link jakarta.servlet.http.HttpServletRequest} e uma
     * {@link ServletRequest}).</p>
     */
    public static class TokenFilter implements Filter {

        private final SecurityProperties properties;

        public TokenFilter(SecurityProperties properties) {
            this.properties = properties;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            String token = extrairToken(req);
            if (token == null || !properties.getTokensValidos().contains(token)) {
                resposta401(res);
                return;
            }
            autenticar(req);
            chain.doFilter(req, res);
        }

        /**
         * Extrai o token da cabecalho {@code Authorization} (formato
         * {@code Bearer <token>}) ou da cabecalho {@code X-API-TOKEN}.
         *
         * @param request a requisicao
         * @return o token, ou {@code null} se ausente
         */
        private String extrairToken(HttpServletRequest request) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                return authorization.substring("Bearer ".length()).trim();
            }
            String apiToken = request.getHeader("X-API-TOKEN");
            if (apiToken != null) {
                return apiToken.trim();
            }
            return null;
        }

        /**
         * Autentica o contexto de seguranca com um principal simples
         * quando o token e valido, para que a cadeia de seguranca autorize
         * a requisicao.
         *
         * @param request a requisicao
         */
        private void autenticar(HttpServletRequest request) {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }
            String token = extrairToken(request);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    token, "secret", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        /**
         * Responde 401 JSON quando o token esta ausente ou invalido.
         *
         * @param response a resposta
         */
        private void resposta401(HttpServletResponse response) {
            try {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write(
                        "{\"codigo\":401,\"mensagem\":\"token de autenticacao invalido ou ausente\"}");
            } catch (IOException exception) {
                throw new RuntimeException("Erro ao escrever resposta de autenticacao", exception);
            }
        }
    }
}
