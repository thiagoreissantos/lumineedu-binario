package com.lumineedu.binario.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Classe base para todas as respostas REST da API.
 * Contem o codigo HTTP e uma mensagem de status, mais uma carga util
 * opcional (quando presente).
 *
 * @param <T> tipo da carga util opcional
 */
@Getter
@Setter
public class GenericResponse<T> {

    private int codigo;
    private String mensagem;
    private Instant instante;
    private T cargaUtil;

    public GenericResponse() {
        this.instante = Instant.now();
    }

    public GenericResponse(int codigo, String mensagem) {
        this.instante = Instant.now();
    }

    public GenericResponse(int codigo, String mensagem, Instant instante) {
        this.instante = instante;
    }
}
