package com.lumineedu.binario.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ArquivoResponseTest {

    @Test
    void deveExporUltimaLeituraEQuantidadeLeiturasAoMapearEntidade() {
        // Preparacao
        ArquivoResponse resposta = new ArquivoResponse();
        Instant ultimaLeitura = Instant.parse("2023-05-01T10:20:30Z");
        resposta.setId(5L);
        resposta.setUltimaLeitura(ultimaLeitura);
        resposta.setQuantidadeLeituras(7L);

        // Assert
        assertThat(resposta.getUltimaLeitura()).isEqualTo(ultimaLeitura);
        assertThat(resposta.getQuantidadeLeituras()).isEqualTo(7L);
    }

    @Test
    void deveFormatarUltimaLeituraNoPadraoDeSaoPaulo() {
        // Preparacao: Sao_Paulo e UTC-3, entao um Instant que se exibe como
        // 10:20:30 local equivale a 13:20:30 UTC.
        ArquivoResponse resposta = new ArquivoResponse();
        Instant ultimaLeitura = Instant.parse("2023-05-01T13:20:30Z");
        resposta.setUltimaLeitura(ultimaLeitura);

        // Acao
        String formatada = resposta.formatarUltimaLeitura();

        // Assert
        assertThat(formatada).isEqualTo("01/05/2023 10:20:30");
    }

    @Test
    void deveRetornarNuloQuandoUltimaLeituraNaoEstabelecida() {
        // Preparacao
        ArquivoResponse resposta = new ArquivoResponse();

        // Assert
        assertThat(resposta.formatarUltimaLeitura()).isNull();
    }
}
