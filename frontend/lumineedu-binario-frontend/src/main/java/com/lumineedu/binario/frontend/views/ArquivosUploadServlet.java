package com.lumineedu.binario.frontend.views;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.lumineedu.binario.frontend.dto.ArquivoDTO;
import com.lumineedu.binario.frontend.security.Auth;
import com.lumineedu.binario.frontend.service.web.WebClientService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Upload de arquivos para a tela de arquivos da interface (tela de arquivos).
 * <p>
 * Servlet bruto (sem Vaadin) que recebe o formulario multipart enviado pelo
 * navegador (submit de um formulario nativo, sem trigger Java), codifica o
 * conteudo de cada arquivo em Base64 e o inclui no backend via
 * {@link WebClientService#incluir(com.lumineedu.binario.frontend.dto.ArquivoDTO,
 * String, String)}. Ao final, redireciona o navegador com status 302 e guarda a
 * mensagem (texto e cor) em atributos da sessao, que a
 * {@link com.lumineedu.binario.frontend.views.components.PainelArquivos} le ao
 * reinserir na UI.
 * <p>
 * Este servlet e' registrado antes do {@code VaadinServlet} (ordem de
 * inicializacao), contorna a protecao CSRF da interface e e' gerenciado pelo
 * Spring como um @Component, sendo registrado no container com o mapeamento
 * "/arquivos/upload" via ServletRegistrationBean.
 */
//@Component
public class ArquivosUploadServlet extends HttpServlet {

    /** Atributo da sessao que transporta a mensagem apos o upload. */
    private static final String ATTR_STATUS = "arquivos.status";
    /** Atributo da sessao que transporta a mensagem apos o upload (redundante). */
    private static final String ATTR_MENSAGEM = "arquivos.message";
    /** Atributo da sessao que transporta a cor da mensagem apos o upload. */
    private static final String ATTR_COR = "arquivos.messageColor";
    /** Atributo da sessao que transporta a quantidade de arquivos enviados. */
    private static final String ATTR_QTD = "arquivos.qtd";

    /** Cor (hex) de sucesso. */
    private static final String COR_SUCESSO = "#1a7f37";
    /** Cor (hex) de erro. */
    private static final String COR_ERRO = "#cf222e";

    /** Nome do campo multipart que transporta os arquivos. */
    private static final String NOME_CAMPO_ARQUIVOS = "arquivos";

    /** Servlet registrado pelo Spring (@link ServletComponentConfig). */
    private final WebClientService webClientService;

    public ArquivosUploadServlet(WebClientService webClientService) {
        this.webClientService = webClientService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String token = tokenDoCookie(request);

            List<Part> partes = new ArrayList<>();
            for (Part part : request.getParts()) {
                if (NOME_CAMPO_ARQUIVOS.equals(part.getName())) {
                    partes.add(part);
                }
            }
            if (partes.isEmpty()) {
                mensagemErro(request, response, "Nenhum arquivo foi enviado.");
                return;
            }

            int total = 0;
            for (Part part : partes) {
                String nome = part.getSubmittedFileName() != null
                        ? URLDecoder.decode(part.getSubmittedFileName(), StandardCharsets.UTF_8)
                        : "arquivo";
                byte[] conteudo = lerConteudo(part);
                if (conteudo == null) {
                    mensagemErro(request, response, "Nao foi possivel ler o arquivo " + nome + ".");
                    continue;
                }

                ArquivoDTO dto = new ArquivoDTO();
                dto.setNomeOriginal(nome);
                dto.setTipoMime(tipoMimeDoPart(part));
                dto.setDescricao(null);
                dto.setConteudoBase64(Base64.getEncoder().encodeToString(conteudo));

                webClientService.incluir(dto, Base64.getEncoder().encodeToString(conteudo), token);
                total++;
            }

            String mensagem = total > 0
                    ? total + " arquivo(s) adicionado(s) aos arquivos."
                    : "Nenhum arquivo foi enviado.";
            String cor = total > 0 ? COR_SUCESSO : COR_ERRO;

            sessao(request).setAttribute(ATTR_STATUS, mensagem);
            sessao(request).setAttribute(ATTR_MENSAGEM, mensagem);
            sessao(request).setAttribute(ATTR_COR, cor);
            sessao(request).setAttribute(ATTR_QTD, total);

            response.sendRedirect("/arquivos");
        } catch (Exception excecao) {
            mensagemErro(request, response, "Erro ao enviar o arquivo: " + excecao.getMessage());
        }
    }

    /**
     * Lê o token de autenticacao da interface do cookie da requisição HTTP.
     * <p>
     * O {@link Auth#getToken()} nao funciona fora de uma requisição Vaadin
     * (a sessao Vaadin nao existe neste contexto), por isso o token e lido
     * diretamente do cookie {@link Auth#COOKIE_NAME}.
     *
     * @param request requisição HTTP.
     * @return o token, ou {@code null} se a requisição nao trouxer cookie.
     */
    private String tokenDoCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Auth.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Lê o tipo MIME (Content-Type) de um {@link Part}. Retorna
     * {@code application/octet-stream} se o part nao informar o tipo.
     *
     * @param part part binario.
     * @return o tipo MIME do part.
     */
    private String tipoMimeDoPart(Part part) {
        String tipoMime = part.getContentType();
        return tipoMime != null ? tipoMime : "application/octet-stream";
    }

    /** Lê todo o conteudo binario de um {@link Part} (ou {@code null} em falha). */
    private byte[] lerConteudo(Part part) {
        try {
            return part.getInputStream().readAllBytes();
        } catch (IOException excecao) {
            throw new UncheckedIOException(excecao);
        }
    }

    /** Retorna a {@link jakarta.servlet.http.HttpSession} da requisição (criando-a se necessario). */
    private jakarta.servlet.http.HttpSession sessao(HttpServletRequest request) {
        return request.getSession(true);
    }

    /** Registra uma mensagem de erro em um atributo da sessao e cor vermelha. */
    private void mensagemErro(HttpServletRequest request, HttpServletResponse response, String mensagem) {
        sessao(request).setAttribute(ATTR_STATUS, mensagem);
        sessao(request).setAttribute(ATTR_MENSAGEM, mensagem);
        sessao(request).setAttribute(ATTR_COR, COR_ERRO);
        sessao(request).setAttribute(ATTR_QTD, 0);
    }
}
