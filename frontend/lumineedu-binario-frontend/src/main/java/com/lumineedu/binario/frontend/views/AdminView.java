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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
public class AdminView extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private final AuthService authService;
    private final AppProperties props;
    private final Component content;
    private final VerticalLayout sideNav;
    private final Span systemName;
    private final Button userMenu;
    private final ApiExceptionHandler exceptionHandler;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authService.estaAutenticado()) {
            event.rerouteTo(LoginView.class);
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
        Button dashboard = new Button("Dashboard", e -> UI.getCurrent().navigate(DashboardView.class));

        Button arquivos = new Button("Arquivos", e -> UI.getCurrent().navigate(ArquivosView.class));

        Button estatisticas = new Button("Estatísticas", e -> UI.getCurrent().navigate(EstatisticasView.class));

        this.sideNav.add(dashboard, arquivos, estatisticas);

        // Barra superior.
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.addClassName("top-bar");
        topBar.setWidthFull();
        topBar.getElement().getStyle().set("padding", "0 0 8px 0");

        this.systemName = new Span();
        this.systemName.setText(props.getUiName());
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

        Span s = new Span();
        s.setText("Sistema");
        topBar.add(new Header(s), spacer(), userMenu);

        // Conteudo (uma tela por vez).
        this.content = new Div();

        this.add(topBar, sideNav, content);
    }

    private static Component spacer() {
        return new Span();
    }
}
