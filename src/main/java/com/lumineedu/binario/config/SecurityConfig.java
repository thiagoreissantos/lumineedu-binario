package com.lumineedu.binario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.lumineedu.binario.security.TokenFilterConfig;
import com.lumineedu.binario.security.TokenFilterConfig.TokenFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Configuracao central da seguranca. Habilita o Spring Security,
 * desativa o CSRF e a criptografia de sessao (nao aplicaveis a este
 * servico stateless), autoriza os endpoints protegidos por token e
 * define os pontos de entrada de erro. A validacao do token e feita
 * pelo filtro {@link TokenFilter}, registrado separadamente, que
 * autentica o contexto de seguranca quando o token e valido.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final TokenFilterConfig.TokenFilter tokenFilter;

    /**
     * Define a cadeia de filtros de seguranca.
     *
     * @param http o builder de seguranca
     * @throws Exception em caso de erro de configuracao
     */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

	    http
	        .csrf(AbstractHttpConfigurer::disable)

	        .httpBasic(AbstractHttpConfigurer::disable)

	        .formLogin(AbstractHttpConfigurer::disable)

	        .sessionManagement(session ->
	            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	        )

	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers("/actuator/**").permitAll()
	            .requestMatchers("/h2-console/**").permitAll()
	            .anyRequest().authenticated()
	        )

	        .headers(headers ->
	            headers.frameOptions(frame -> frame.sameOrigin())
	        )

	        .exceptionHandling(ex -> {

	            ex.authenticationEntryPoint((request, response, authException) -> {
	                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	                response.setContentType("application/json; charset=UTF-8");

	                response.getWriter().write(
	                    "{\"codigo\":401,\"mensagem\":\"token de autenticacao invalido ou ausente\"}"
	                );
	            });

	            ex.accessDeniedHandler((request, response, accessDenied) -> {
	                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	                response.setContentType("application/json; charset=UTF-8");

	                response.getWriter().write(
	                    "{\"codigo\":403,\"mensagem\":\"acesso nao autorizado\"}"
	                );
	            });
	        })

	        .addFilterBefore(tokenFilter, AnonymousAuthenticationFilter.class);

	    return http.build();
	}
}
