package com.lumineedu.binario.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.service.AuthService;

/** Tela de login da interface. */
@Route(value = "", layout = LoginLayout.class)
public class LoginView extends VerticalLayout {
    private final TextField usuario;
    private final PasswordField senha;
    private final Span mensagemErro;
    private final AuthService authService;

    public LoginView(AppProperties props, AuthService authService) {
        this.authService = authService;
        addClassName("login-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Div brandPanel = new Div();
        brandPanel.addClassName("login-brand-panel");
        Span brandMark = new Span("LE");
        brandMark.addClassName("brand-mark");
        H2 brandName = new H2(props.getUiName());
        brandName.addClassName("login-brand-name");
        Paragraph brandDescription = new Paragraph("Gerenciamento seguro de arquivos binários.");
        brandDescription.addClassName("login-brand-description");
        Span featureOne = new Span("✓  Seguro e confiável");
        Span featureTwo = new Span("▤  Armazenamento centralizado");
        Span featureThree = new Span("↗  Acesso rápido e organizado");
        featureOne.addClassName("brand-feature");
        featureTwo.addClassName("brand-feature");
        featureThree.addClassName("brand-feature");
        brandPanel.add(brandMark, brandName, brandDescription, featureOne, featureTwo, featureThree);

        Div formPanel = new Div();
        formPanel.addClassName("login-form-panel");
        H2 title = new H2("Acesso ao sistema");
        title.addClassName("login-title");
        Paragraph subtitle = new Paragraph("Entre com suas credenciais para continuar.");
        subtitle.addClassName("login-subtitle");

        usuario = new TextField("Usuário");
        usuario.setPlaceholder("Digite seu usuário");
        usuario.setRequiredIndicatorVisible(true);
        usuario.setErrorMessage("Informe o usuário");
        usuario.setWidthFull();
        usuario.addClassName("login-input");

        senha = new PasswordField("Senha");
        senha.setPlaceholder("Digite sua senha");
        senha.setRequiredIndicatorVisible(true);
        senha.setErrorMessage("Informe a senha");
        senha.setWidthFull();
        senha.addClassName("login-input");
        senha.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> entrarNoSistema());

        mensagemErro = new Span();
        mensagemErro.addClassName("login-error");
        Button entrar = new Button("Entrar", e -> entrarNoSistema());
        entrar.addClassName("login-submit");
        entrar.setWidthFull();

        formPanel.add(title, subtitle, usuario, senha, mensagemErro, entrar);
        Div shell = new Div(brandPanel, formPanel);
        shell.addClassName("login-shell");
        add(shell);
    }

    private void entrarNoSistema() {
        if (usuario.isEmpty() || senha.isEmpty()) {
            mensagemErro.setText("Informe o usuário e a senha para continuar.");
            return;
        }
        if (authService.login(usuario.getValue(), senha.getValue())) {
            UI.getCurrent().navigate(DashboardView.class);
        } else {
            mensagemErro.setText("Usuário ou senha inválidos.");
        }
    }
}
