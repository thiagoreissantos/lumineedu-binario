package com.lumineedu.binario.exception;

import java.time.Instant;

import com.lumineedu.binario.dto.GenericResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Objeto padrao retornado em todas as respostas de erro da API.
 * Contem o codigo HTTP, a mensagem de erro e um carimbo de tempo.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ErroResponse<T> extends GenericResponse<T> {

    private String mensagemErro;
    private Instant instanteErro;

    public ErroResponse() {
        super();
        this.instanteErro = Instant.now();
    }

    public ErroResponse(int codigo, String mensagem, Instant instante) {
        super(codigo, mensagem, instante);
    }
}
