package com.lumineedu.binario.frontend.exception;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converte o corpo JSON de erro retornado pelo backend em uma {@link ApiException}.
 * <p>
 * O backend pode devolver dois formatos:
 * <ul>
 *   <li>filtragem de token: {@code {"codigo":401,"mensagem":"..."}}; e</li>
 *   <li>handler global: {@code {"codigo", "mensagem", "instante", "mensagemErro", "instanteErro"}}.</li>
 * </ul>
 * Este conversor extrai {@code codigo} e {@code mensagem} (ou {@code mensagemErro})
 * de qualquer um dos dois formatos.
 */
@Component
public class ApiExceptionConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiException converter(int status, String body) {
        String texto = body == null ? "" : body;
        String mensagem = lerMensagem(texto);
        return new ApiException(status, mensagem);
    }

    /**
     * Extrai a mensagem de erro de um corpo JSON, dando preferencia a
     * {@code mensagemErro} (detalhe) e caindo para {@code mensagem} (resumo).
     */
    public String lerMensagem(String body) {
        if (body == null || body.isBlank()) {
            return body == null ? "" : body;
        }
        try {
            JsonNode raiz = objectMapper.readTree(body);
            if (raiz == null || raiz.isNull()) {
                return body;
            }
            JsonNode mensagemErro = raiz.get("mensagemErro");
            if (mensagemErro != null && !mensagemErro.isNull() && mensagemErro.asText().isBlank()) {
                mensagemErro = null;
            }
            String detalhe = mensagemErro != null ? mensagemErro.asText() : null;
            if (detalhe != null && !detalhe.isBlank()) {
                return detalhe;
            }
            JsonNode mensagem = raiz.get("mensagem");
            if (mensagem != null && !mensagem.isNull()) {
                return mensagem.asText();
            }
            return body;
        } catch (Exception e) {
            return body;
        }
    }

    public String lerTexto(String body) {
        return body == null ? "" : body;
    }

    public byte[] lerBytes(String body) {
        return lerTexto(body).getBytes(StandardCharsets.UTF_8);
    }
}
