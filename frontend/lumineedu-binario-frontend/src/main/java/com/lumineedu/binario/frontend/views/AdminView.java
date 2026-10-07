package com.lumineedu.binario.frontend.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;

import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.service.AuthService;

/**
 * Layout principal da área administrativa.
 * <p>
 * Contem a barra superior (nome do sistema + menu de usuario com acao de sair),
 * o menu lateral (Dashboard, Arquivos, Estatisticas) e a area de conteudo
 * (uma por vez). Nao e uma tela em si: e o layout (RouterLayout) das telas
 * protegidas. Contem a protecao de rotas: ao acessar uma rota protegida sem
 * estar autenticado, redireciona para a tela de login.
 */
public class AdminView extends VerticalLayout implements RouterLayout {

    private final AuthService authService;
    private final AppProperties props;
    private final Component content;
    private final VerticalLayout sideNav;
    private final Span systemName;
    private final Button userMenu;
    private final ApiExceptionHandler exceptionHandler;

    protected void onBeforeEnter(RouteConfiguration configuration) {
        // Protecao de rotas: se nao autenticado, redireciona para o login.
        if (!authService.estaAutenticado()) {
            UI.getCurrent().navigate(LoginView.class);
        }
    }

    public AdminView(AppProperties props, AuthService authService, ApiExceptionHandler exceptionHandler) {
        this.props = props;
        this.authService = authService;
        this.exceptionHandler = exceptionHandler;

        // Menu lateral (apenas visivel na area administrativa, ou seja, apos o login).
        this.sideNav = new VerticalLayout();
        this.sideNav.addClassName("side-nav");
        this.sideNav.addClassName("auto-width");
        this.sideNav.setWidth("240px");

        // Links do menu lateral.
        Anchor dashboard = new Anchor("#dashboard");
        dashboard.setText("Dashboard");
        Anchor arquivos = new Anchor("#arquivos");
        arquivos.setText("Arquivos");
        Anchor estatisticas = new Anchor("#estatisticas");
        estatisticas.setText("Estatisticas");
        this.sideNav.add(dashboard, arquivos, estatisticas);

        // Barra superior.
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.addClassName("top-bar");
        topBar.setWidthFull();
        topBar.getElement().getStyle().set("padding", "0 0 8px 0");

        this.systemName = new Span(props.getUiName());
        this.systemName.getElement().getStyle()
                .set("font-weight", "bold");

        this.userMenu = new Button();
        this.userMenu.getElement().setAttribute("title", "Menu do usuário");
        this.userMenu.addClickListener(e -> {
            if (authService.estaAutenticado()) {
                authService.logout();
                UI.getCurrent().navigate(LoginView.class);
            }
        });

        topBar.add(new Header(new Span("Sistema")), spacer(), userMenu);

        // Conteudo (uma tela por vez).
        this.content = new Div();

        this.add(topBar, sideNav, content);
    }

    private static Component spacer() {
        return new Span();
    }
}
