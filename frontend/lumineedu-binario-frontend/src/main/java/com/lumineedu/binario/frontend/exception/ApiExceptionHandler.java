package com.lumineedu.binario.frontend.exception;

import java.util.Arrays;

import com.vaadin.flow.component.html.Span;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Traduz um {@link ApiException} (com o codigo HTTP do backend) em uma mensagem
 * amigavel, sem nunca expor a stack trace ao usuario.
 * <p>
 * As mensagens sao genericas e seguras para exibicao no navegador; os detalhes
 * especificos ficam nos logs do servidor.
 */
@Component
public class ApiExceptionHandler {


    /** Palavras-chave que indicam uma mensagem potencialmente sensivel no backend. */
    private static final String[] SENSITIVE = {
            "token", "senha", "password", "credencial", "credenciais", "stack trace", "exception", "trace" };

    /**
     * Retorna a mensagem amigavel para o codigo HTTP informado.
     *
     * @param status codigo HTTP retornado pelo backend
     * @return mensagem amigavel em portugues do Brasil
     */
    public String mensagemAmigavel(int status) {
        switch (HttpStatus.valueOf(status)) {
            case UNAUTHORIZED:
                return "Sessao expirada ou token invalido. Faça login novamente.";
            case FORBIDDEN:
                return "Você não tem permissão para acessar isto.";
            case NOT_FOUND:
                return "Registro não encontrado.";
            case CONFLICT:
                return "Este registro já existe.";
            case PAYLOAD_TOO_LARGE:
                return "O arquivo ou a solicitacao é muito grande. Tente um arquivo menor.";
            case BAD_REQUEST:
                return "Solicitacao inválida. Verifique os dados informados.";
            case INTERNAL_SERVER_ERROR:
                return "Ocorreu um erro no servidor. Tente novamente em alguns instantes.";
            default:
                return "Ops! Algo deu errado. Tente novamente mais tarde.";
        }
    }

    /**
     * Monta a mensagem amigavel exibida ao usuario para uma {@link ApiException}.
     * <p>
     * Prefere a mensagem bruta do backend apenas quando ela for util e nao
     * sensivel; caso contrario usa a mensagem amigavel padrao baseada no codigo
     * HTTP. Nunca expoe stack traces ou segredos.
     *
     * @param excecao excecao API (com codigo HTTP e mensagem bruta)
     * @return mensagem amigavel em portugues do Brasil
     */
    public String amigavel(ApiException excecao) {
        String amigavel = mensagemAmigavel(excecao.getStatus());
        String bruta = excecao.getMensagem();
        boolean segura = bruta != null && !bruta.isBlank()
                && !Arrays.stream(SENSITIVE)
                        .anyMatch(bruta.toLowerCase()::contains);
        return segura ? bruta : amigavel;
    }

    /**
     * Exibe uma mensagem ao usuario sobre um componente, sem expor detalhes
     * tecnicos.
     *
     * @param component componente onde a mensagem sera exibida (ex.: a grid).
     * @param mensagem  texto amigavel a exibir.
     */
    public void mostrarMensagem(com.vaadin.flow.component.Component component, String mensagem) {
        if (component == null || mensagem == null || mensagem.isBlank()) {
            return;
        }
        Span span = new Span();
        span.setText(mensagem);
        component.getElement().appendChild(span.getElement());
    }

    /**
     * Monta a mensagem final exibida ao usuario (sinonimo de {@link #amigavel(ApiException)}).
     */
    public String aplicar(ApiException excecao) {
        return amigavel(excecao);
    }

    /**
     * Formata um instante do backend para exibicao.
     */
    public String formatarInstante(java.time.Instant instante) {
        return instante == null ? "" : instante.toString();
    }
}
