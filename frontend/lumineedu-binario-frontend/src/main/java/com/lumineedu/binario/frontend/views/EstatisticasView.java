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
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;
        this.cargaUtil = new Span("0");
        this.tamanhoTotal = new Span("0 B");
        this.instante = new Span("--");

        this.add(new Header(new Span(props.getUiName() + " - Estatisticas")));

        // Total de arquivos.
        HorizontalLayout cardCarga = new HorizontalLayout();
        cardCarga.add(new Span("Total de Arquivos"), this.cargaUtil);

        // Tamanho total.
        HorizontalLayout cardTamanho = new HorizontalLayout();
        cardTamanho.add(new Span("Tamanho Total"), this.tamanhoTotal);

        // Instante (ultima atualizacao).
        HorizontalLayout cardInstante = new HorizontalLayout();
        cardInstante.add(new Span("Ultima Atualizacao"), this.instante);

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
