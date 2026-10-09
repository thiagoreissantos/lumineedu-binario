package com.lumineedu.binario.frontend.views;

import com.vaadin.flow.component.html.Header;
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
 * Tela de estatisticas detalhadas do backend.
 * <p>
 * Exibe as estatisticas reais do backend (total de arquivos, tamanho total,
 * instante, etc.) em cards, sem armazenar nenhum dado local.
 */
@Route(value = "estatisticas", layout = AdminView.class)
public class EstatisticasView extends VerticalLayout {

    private final EstatisticaService estatisticaService;
    private final ApiExceptionHandler exceptionHandler;
    private final Span cargaUtil;
    private final Span tamanhoTotal;
    private final Span instante;

    public EstatisticasView(AppProperties props, EstatisticaService estatisticaService, ApiExceptionHandler exceptionHandler) {
        addClassName("page-view");
        addClassName("statistics-view");
        setWidthFull();
        setPadding(true);
        setSpacing(true);
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;
        this.cargaUtil = new Span();
        this.cargaUtil.setText("0");
        this.tamanhoTotal = new Span();
        this.tamanhoTotal.setText("0 B");
        this.instante = new Span();
        this.instante.setText("--");

        Span span = new Span();
        span.setText(props.getUiName() + " - Estatisticas");
        Header heading = new Header(span);
        heading.addClassName("page-heading");
        this.add(heading);

        // Total de arquivos.
        HorizontalLayout cardCarga = new HorizontalLayout();
        Span label = new Span();
        label.setText("Total de Arquivos");
        cardCarga.addClassName("metric-card");
        label.addClassName("metric-label");
        this.cargaUtil.addClassName("metric-value");
        cardCarga.add(label, this.cargaUtil);

        // Tamanho total.
        HorizontalLayout cardTamanho = new HorizontalLayout();
        Span labelTamanho = new Span();
        labelTamanho.setText("Tamanho Total");
        cardTamanho.addClassName("metric-card");
        labelTamanho.addClassName("metric-label");
        this.tamanhoTotal.addClassName("metric-value");
        cardTamanho.add(labelTamanho, this.tamanhoTotal);

        // Instante (ultima atualizacao).
        HorizontalLayout cardInstante = new HorizontalLayout();
        Span labelInstante = new Span();
        labelInstante.setText("Ultima Atualizacao");
        cardInstante.addClassName("metric-card");
        labelInstante.addClassName("metric-label");
        this.instante.addClassName("metric-value");
        cardInstante.add(labelInstante, this.instante);

        cardCarga.addClassName("stat-card");
        cardTamanho.addClassName("stat-card");
        cardInstante.addClassName("stat-card");
        this.add(cardCarga, cardTamanho, cardInstante);
        this.setPadding(true);
        this.setSpacing(true);

        carregar();
    }

    /** Carrega e exibe as estatisticas reais do backend. */
    private void carregar() {
        try {
            EstatisticasResponse res = estatisticaService.obter();
            this.cargaUtil.setText(String.valueOf(res.getCargaUtil()));
            this.tamanhoTotal.setText(formatarBytes(res.getTamanhoTotalBytes()));
            this.instante.setText(res.getInstante() != null
                    ? res.getInstante().toString()
                    : "--");
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
