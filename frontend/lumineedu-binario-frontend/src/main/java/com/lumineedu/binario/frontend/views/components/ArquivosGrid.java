package com.lumineedu.binario.frontend.views.components;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataProviderListener;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.lumineedu.binario.frontend.dto.ArquivoResponse;
import com.lumineedu.binario.frontend.exception.ApiException;
import com.lumineedu.binario.frontend.exception.ApiExceptionHandler;
import com.lumineedu.binario.frontend.service.BinarioService;
import com.lumineedu.binario.frontend.service.EstatisticaService;

/**
 * Grid de listagem de arquivos da interface (carregado pelo backend, paginado
 * e com selecao multiplas), acompanhado de um painel de acoes (remocao).
 * <p>
 * As colunas leem as propriedades do {@code ArquivoResponse} por reflexao (sem
 * getters, que estao ausentes sob compilacao -proc:none) e os dados sao
 * carregados de um {@code DataProvider} customizado que delega ao backend
 * ({@link BinarioService}).
 */
public class ArquivosGrid extends Grid<ArquivoResponse> {

    private final BinarioService binarioService;
    private final EstatisticaService estatisticaService;
    private final ApiExceptionHandler exceptionHandler;

    private static final int TAMANHO_PAGINA = 10;
    private static final int PAGINA_PADRAO = 1;

    private static final String COLOUNA_ID = "id";
    private static final String COLOUNA_NOME_ORIGINAL = "nomeOriginal";
    private static final String COLOUNA_TIPO_MIME = "tipoMime";
    private static final String COLOUNA_DESCRICAO = "descricao";

    private final int paginaCorrente;

    public ArquivosGrid(BinarioService binarioService, EstatisticaService estatisticaService,
            ApiExceptionHandler exceptionHandler) {
        this(binarioService, estatisticaService, exceptionHandler, PAGINA_PADRAO);
    }

    public ArquivosGrid(BinarioService binarioService, EstatisticaService estatisticaService,
            ApiExceptionHandler exceptionHandler, int paginaCorrente) {
        super();
        addClassName("files-grid-component");
        setWidthFull();
        setHeight("auto");
        setAllRowsVisible(true);
        this.binarioService = binarioService;
        this.estatisticaService = estatisticaService;
        this.exceptionHandler = exceptionHandler;
        this.paginaCorrente = paginaCorrente;

        this.adicionarColuna(COLOUNA_NOME_ORIGINAL, "Nome");
        this.adicionarColuna(COLOUNA_TIPO_MIME, "Tipo");
        this.adicionarColuna(COLOUNA_DESCRICAO, "Descricao");

    }

    /** Lista os itens selecionados no grid. */
    public List<ArquivoResponse> seletosGrid() {
        return new ArrayList<>(this.getSelectedItems());
    }

    private void adicionarColuna(String property, String label) {
        ValueProvider<ArquivoResponse, ?> provider = providerPropriedade(property);
        Grid.Column<ArquivoResponse> coluna = this.addColumn(new ColumnPathRenderer<>(property, provider));
        coluna.setHeader(label);
        coluna.setKey(property);
    }

    /**
     * Retorna um {@code ValueProvider} que le a propriedade dada do
     * {@code ArquivoResponse} por reflexao (os getters estao ausentes sob
     * compilacao -proc:none).
     */
    private <SOURCE> ValueProvider<SOURCE, ?> providerPropriedade(String property) {
        Field found = null;
        Class<?> classe = ArquivoResponse.class;
        while (classe != null && found == null) {
            try {
                found = classe.getDeclaredField(property);
            } catch (NoSuchFieldException e) {
                classe = classe.getSuperclass();
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("Sem campo: " + property);
        }
        final Field field = found;
        try {
            field.setAccessible(true);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        }
        return source -> {
            try {
                return field.get((Object) source);
            } catch (IllegalAccessException e) {
                return null;
            }
        };
    }

    /** Retorna o identificador (id) de um arquivo, por reflexao. */
    private Long getIdGrid(ArquivoResponse arquivo) {
        try {
            return (Long) ArquivoResponse.class.getDeclaredField("id").get(arquivo);
        } catch (Exception e) {
            return null;
        }
    }

    /** Carrega os dados do backend (pagina e tamanho) na grid. */
    public void carregar() {
        try {
            DataProvider<ArquivoResponse, Void> dataProvider = new DataProvider<ArquivoResponse, Void>() {
                @Override
                public boolean isInMemory() {
                    return true;
                }
                @Override
                public int size(Query<ArquivoResponse, Void> query) {
                    return binarioService.estatisticas().getCargaUtil().intValue();
                }
                @Override
                public Stream<ArquivoResponse> fetch(Query<ArquivoResponse, Void> query) {
                    PageRequest sort = PageRequest.of(query.getPage(), query.getLimit());
                    Page<ArquivoResponse> pagina = binarioService.listarPage(sort, null);
                    return pagina.getContent().stream();
                }
                @Override
                public void refreshItem(ArquivoResponse item) {
                    throw new UnsupportedOperationException();
                }
                @Override
                public void refreshAll() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public Registration addDataProviderListener(DataProviderListener<ArquivoResponse> listener) {
                    return new Registration() {
                        @Override
                        public void remove() {
                            // no-op: sem listeners de eventos neste data provider
                        }
                    };
                }
            };
            this.setItems(dataProvider);
        } catch (ApiException excecao) {
            exceptionHandler.mostrarMensagem(this, exceptionHandler.amigavel(excecao));
        }
    }

    /** Remove um arquivo por identificador. */
    public void remover(ArquivoResponse arquivo) {
        Long id = getIdGrid(arquivo);
        if (id != null) {
            binarioService.remover(id);
        }
    }

    /** Remove todos os itens selecionados no grid. */
    public void removerSeletosGrid() {
        for (ArquivoResponse arquivo : this.seletosGrid()) {
            this.remover(arquivo);
        }
    }
}
