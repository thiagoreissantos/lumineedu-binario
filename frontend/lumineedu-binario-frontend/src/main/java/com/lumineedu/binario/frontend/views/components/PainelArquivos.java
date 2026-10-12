package com.lumineedu.binario.frontend.views.components;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.service.BinarioService;
import com.lumineedu.binario.frontend.service.EstatisticaService;

/**
 * Painel da tela de arquivos: formulario nativo de upload (botao submit com
 * input de arquivos oculto de sobreposicao) e grid de listagem, com span de
 * status que exibe a mensagem enviada pelo servidor apos o upload.
 * <p>
 * Nao ha trigger de envio em Java: o formulario nativo
 * (action="/arquivos/upload", enctype="multipart/form-data") e submetido pelo
 * navegador. Ao final, o servidor redireciona (302) e guarda a mensagem (texto
 * e cor) em atributos da sessao, que este painel le ao reinserir e exibe.
 */
public class PainelArquivos extends VerticalLayout {

    private final BinarioService binarioService;
    private final EstatisticaService estatisticaService;
    private final ApiExceptionHandler exceptionHandler;

    private final Span status;
    private final ArquivosGrid arquivosGrid;

    /** Cor (hex) padrao para o status quando nenhum atributo de sessao estiver presente. */
    private static final String COR_PADRAO = "#8a8a8a";

    public PainelArquivos(BinarioService binarioService, EstatisticaService estatisticaService,
            ApiExceptionHandler exceptionHandler) {
        this.binarioService = binarioService;
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;

        this.status = lerStatus();
        add(criarFormulario());
        add(this.status);
        this.arquivosGrid = new ArquivosGrid(binarioService, estatisticaService, exceptionHandler);
        add(arquivosGrid);
    }

    /**
     * Cria o formulario nativo de upload: um botao submit estilizado (com o
     * icone de upload) e um input de arquivos nativo oculto de sobreposicao,
     * que ao ser clicado abre o seletor de arquivos e, ao confirmar, submete o
     * formulario para o endpoint "/arquivos/upload".
     */
    private VerticalLayout criarFormulario() {
        VerticalLayout layout = new VerticalLayout();
        layout.getElement().getStyle().set("padding", "8px");

        Element form = new Element("form");
        form.setAttribute("action", "/arquivos/upload");
        form.setAttribute("method", "POST");
        form.setAttribute("enctype", "multipart/form-data");
        form.getStyle().set("position", "relative");

        Element botao = new Element("button");
        botao.setAttribute("type", "submit");
        botao.getStyle().set("width", "100%").set("height", "100%")
                .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                .set("border", "1px dashed #bbb").set("border-radius", "8px")
                .set("background", "transparent").set("cursor", "pointer");
        Span icone = new Span(VaadinIcon.UPLOAD.create());
        botao.appendChild(icone.getElement());
        form.appendChild(botao);

        Element inputArquivos = new Element("input");
        inputArquivos.setAttribute("type", "file");
        inputArquivos.setAttribute("multiple", "true");
        inputArquivos.setAttribute("name", "arquivos");
        inputArquivos.getStyle().set("position", "absolute")
                .set("inset", "0").set("opacity", "0").set("cursor", "pointer");
        form.appendChild(inputArquivos);

        layout.getElement().appendChild(form);
        return layout;
    }

    /**
     * Lê na UI o status de upload guardado em atributos da sessao pelo servidor,
     * e o exibe no span. A sessao e lida via {@link UI#getCurrent()} (nao pela
     * view), pois o servlet de upload (raw servlet) escreve os atributos na
     * sessao do cliente apos o redirect 302.
     *
     * @return span com a mensagem de status, ou um span vazio se nao houver mensagem.
     */
    private Span lerStatus() {
        Span span = new Span();
        VaadinSession session = UI.getCurrent().getSession();
        if (session != null) {
            Object valor = session.getAttribute("arquivos.status");
            String mensagem = valor != null ? valor.toString() : null;
            Object cor = session.getAttribute("arquivos.messageColor");
            String corHex = cor != null ? cor.toString() : COR_PADRAO;
            if (mensagem != null && !mensagem.isBlank()) {
                span.setText(mensagem);
                span.getStyle().set("color", corHex).set("font-style", "italic");
            }
        }
        return span;
    }
}
