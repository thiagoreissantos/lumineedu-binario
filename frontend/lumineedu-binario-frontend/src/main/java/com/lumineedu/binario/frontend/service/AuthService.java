package com.lumineedu.binario.frontend.service;

import org.springframework.stereotype.Service;

import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.security.Auth;

/**
 * Autenticacao da interface (frontend-only).
 * <p>
 * A interface nao valida credenciais contra o backend: possui uma autenticação
 * simples e independente. Valida o usuario e a senha informados (comparando
 * contra as credenciais simples da interface em {@link AppProperties#getSecurity()})
 * e, apos o login, utiliza o token fixo que o backend aceita em seus
 * endpoints. O logout e uma acao do frontend que remove o token local (sem
 * chamada ao backend).
 * <p>
 * Nao armazena nenhuma credencial de usuario nem token sensivel em disco nem
 * em {@code application.properties}.
 */
@Service
public class AuthService {

    /**
     * Token fixo que a interface utiliza para se comunicar com o backend.
     * E um token valido do backend (tokens-validos), nunca uma credencial de
     * usuario nem algo sensivel.
     */
    public static final String TOKEN_API = "token-secreto-lumine";

    private final AppProperties props;
    private final Auth auth;

    public AuthService(AppProperties props, Auth auth) {
        this.props = props;
        this.auth = auth;
    }

    /**
     * Valida as credenciais da interface e, em caso de sucesso, armazena o token
     * fixo de API no cookie da interface.
     *
     * @param usuario usuario informado.
     * @param senha   senha informada.
     * @return {@code true} se o login teve sucesso, {@code false} caso contrario.
     */
    public boolean login(String usuario, String senha) {
        if (!validar(usuario, senha)) {
            return false;
        }
        auth.setToken(TOKEN_API);
        return true;
    }

    /**
     * Invalida a sessao da interface (logout). Remove o token local (sem
     * chamada ao backend).
     */
    public void logout() {
        auth.desautenticar();
    }

    /**
     * Indica se o usuario esta autenticado na interface.
     *
     * @return {@code true} se o usuario esta autenticado.
     */
    public boolean estaAutenticado() {
        return auth.estaAutenticado();
    }

    /**
     * Retorna o token de API atual da interface (nao sensivel).
     *
     * @return o token de API atual.
     */
    public String getToken() {
        return auth.getToken();
    }

    /**
     * Valida as credenciais da interface (frontend-only).
     *
     * @param usuario usuario informado.
     * @param senha   senha informada.
     * @return {@code true} se as credenciais sao validas.
     */
    private boolean validar(String usuario, String senha) {
        AppProperties.Security seg = props.getSecurity();
        return seg.getUsername() != null && seg.getPassword() != null
                && seg.getUsername().equals(usuario)
                && seg.getPassword().equals(senha);
    }
}
