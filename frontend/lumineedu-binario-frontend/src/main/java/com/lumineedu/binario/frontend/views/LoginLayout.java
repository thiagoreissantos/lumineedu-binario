package com.lumineedu.binario.frontend.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.component.dependency.CssImport;

/** Layout da tela de login. */
@CssImport("./styles/lumineedu.css")
public class LoginLayout extends VerticalLayout implements RouterLayout {
    public LoginLayout() {
        addClassName("login-layout");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }
}
