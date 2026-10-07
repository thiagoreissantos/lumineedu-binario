package com.lumineedu.binario.frontend.views;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import com.lumineedu.binario.frontend.config.AppProperties;
import com.lumineedu.binario.frontend.dto.EstatisticasResponse;
import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.exception.ApiException;
import com.lumineedu.binario.frontend.service.EstatisticaService;

/**
 * Tela principal do painel (Dashboard).
 * <p>
 * Mostra apenas as estatisticas reais do backend (cards) e um resumo, sem
 * armazenar nenhum dado local. A gestao completa de arquivos (Grid + busca +
 * paginacao) fica em {@link ArquivosView}.
 */
@Route(value = "dashboard", layout = AdminView.class)
public class DashboardView extends VerticalLayout {

    private final Header header;
    private final Button refreshButton;
    private final Map<String, Span> cards;
    private final EstatisticaService estatisticaService;
    private final ApiExceptionHandler exceptionHandler;

    public DashboardView(AppProperties props, EstatisticaService estatisticaService, ApiExceptionHandler exceptionHandler) {
        this.header = new Header(new Span("Dashboard"));
        this.refreshButton = new Button("Atualizar", e -> carregar());
        this.cards = new HashMap<>();
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;

        // Carga util (numero de arquivos).
        Span cargaUtil = new Span();
        this.cards.put("cargaUtil", cargaUtil);

        // Tamanho total (em bytes).
        Span tamanhoTotal = new Span();
        this.cards.put("tamanhoTotalBytes", tamanhoTotal);

        // Resumo.
        Span resumo = new Span(props.getUiName());
        resumo.getStyle().set("color", "#8a8a8a").set("font-style", "italic");
        Paragraph paragrafo = new Paragraph(resumo);

        // Layout.
        this.add(this.header);
        this.add(this.refreshButton);
        for (Span span : cards.values()) {
            this.add(span);
        }
        this.add(paragrafo);
        this.setPadding(true);
        this.setSpacing(true);

        carregar();
    }

    /** Carrega as estatisticas e atualiza os cards e o resumo do backend. */
    private void carregar() {
        try {
            EstatisticasResponse res = estatisticaService.obter();
            cards.get("cargaUtil").setText(String.valueOf(res.getCargaUtil()));
            cards.get("tamanhoTotalBytes").setText(formatarBytes(res.getTamanhoTotalBytes()));
        } catch (ApiException excecao) {
            exceptionHandler.mostrarMensagem(this, exceptionHandler.amigavel(excecao));
        }
    }

    /** Formata um tamanho em bytes para uma string amigavel (ex.: 1.5 KB). */
    private static String formatarBytes(long bytes) {
        if (bytes == 0) {
            return "0 B";
        }
        long kb = bytes / 1024;
        if (kb >= 1024) {
            long mb = kb / 1024;
            if (mb >= 1024) {
                long gb = mb / 1024;
                return String.format("%.2f GB", gb / 1024.0);
            }
            return String.format("%.2f MB", mb);
        }
        return String.format("%.2f KB", kb);
    }
}
