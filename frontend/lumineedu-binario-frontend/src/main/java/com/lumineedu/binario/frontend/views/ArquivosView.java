package com.lumineedu.binario.frontend.views;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.dto.EstatisticasResponse;
import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.exception.ApiException;
import com.lumineedu.binario.frontend.service.BinarioService;
import com.lumineedu.binario.frontend.service.EstatisticaService;
import com.lumineedu.binario.frontend.views.components.ArquivosGrid;

/**
 * Tela de gestao de arquivos do frontend.
 * <p>
 * Contem o {@link ArquivosGrid} (busca + paginacao no backend + remover) e um
 * resumo do total de arquivos exibido no rodape.
 */
@Route(value = "arquivos", layout = AdminView.class)
public class ArquivosView extends VerticalLayout {

    private final BinarioService binarioService;
    private final EstatisticaService estatisticaService;
    private final ApiExceptionHandler exceptionHandler;
    private final ArquivosGrid arquivosGrid;
    private final Span total;

    public ArquivosView(AppProperties props, BinarioService binarioService, EstatisticaService estatisticaService, ApiExceptionHandler exceptionHandler) {
        addClassName("page-view");
        addClassName("files-view");
        setWidthFull();
        setPadding(true);
        setSpacing(true);
        this.binarioService = binarioService;
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;
        this.arquivosGrid = new ArquivosGrid(binarioService, estatisticaService, exceptionHandler);
        this.total = new Span();
        this.total.setText("0");

        Span span = new Span();
        span.setText(props.getUiName() + " - Arquivos");
        Header heading = new Header(span);
        heading.addClassName("page-heading");
        this.add(heading);
        arquivosGrid.addClassName("files-grid");
        this.add(arquivosGrid);
        this.setPadding(true);
        this.setSpacing(true);
        atualizarResumo();
    }

    /** Atualiza o resumo do total de arquivos exibido no rodape. */
    private void atualizarResumo() {
        try {
            EstatisticasResponse res = estatisticaService.obter();
            this.total.setText(String.valueOf(res.getCargaUtil()));
        } catch (ApiException excecao) {
            exceptionHandler.mostrarMensagem(this, exceptionHandler.amigavel(excecao));
        }
        Paragraph rodape = new Paragraph();
        rodape.setText("Total: " + total + " arquivo(s).");
        rodape.getStyle().set("color", "#8a8a8a").set("font-style", "italic");
        rodape.getStyle().set("textAlign", "right");
        this.add(rodape);
        rodape.addClassName("page-footnote");
        this.setPadding(true);
    }
}
