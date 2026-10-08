package com.lumineedu.binario.frontend.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;

import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.service.AuthService;

/**
 * Tela de login da interface.
 * <p>
 * Valida as credenciais simples da interface (frontend-only) e, em caso de
 * sucesso, redireciona para a area administrativa. Em caso de falha, mostra uma
 * mensagem de erro. Nao faz chamadas HTTP ao backend.
 */
@Route(value = "", layout = LoginLayout.class)
public class LoginView extends VerticalLayout {

    private final TextField usuario;
    private final PasswordField senha;
    private final Button entrar;
    private final Span mensagemErro;
    private final AuthService authService;

    public LoginView(AppProperties props, AuthService authService) {
        this.authService = authService;

        // Campos de login.
        this.usuario = new TextField("Usuário");
        this.usuario.setRequiredIndicatorVisible(true);
        this.usuario.setErrorMessage("Campo obrigatório");

        this.senha = new PasswordField("Senha");
        this.senha.setRequiredIndicatorVisible(true);
        this.senha.setErrorMessage("Campo obrigatório");

        // Botão de login.
        this.entrar = new Button("Entrar", e -> entrarNoSistema());

        // Mensagem de erro.
        this.mensagemErro = new Span();
        this.mensagemErro.getStyle().set("color", "#cf222e");

        // Layout.
        HorizontalLayout campos = new HorizontalLayout();
        campos.add(new Span(props.getUiName()), spacer(), new VerticalLayout(usuario, senha));
        campos.setSpacing(true);
        campos.setPadding(true);

        VerticalLayout container = new VerticalLayout();
        container.add(new Header(new Span("Acesso ao sistema")));
        container.add(campos);
        container.add(mensagemErro);
        container.add(entrar);
        container.setPadding(true);
        container.setSpacing(true);
        container.addClassName("login-container");

        this.add(container);
    }

    /**
     * Valida os campos e executa o login.
     */
    private void entrarNoSistema() {
        if (usuario.isEmpty() || senha.isEmpty()) {
            mensagemErro.setText("Usuário ou senha inválidos.");
            return;
        }
        if (authService.login(usuario.getValue(), senha.getValue())) {
            UI.getCurrent().navigate(DashboardView.class);
        } else {
            mensagemErro.setText("Usuário ou senha inválidos.");
        }
    }

    private static Component spacer() {
        return new Span();
    }
}
