package com.lumineedu.binario.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * Testes unitarios do metodo que registra uma leitura (recuperacao) de
 * um arquivo: atualizacao do timestamp da ultima leitura e incremento do
 * contador de leituras.
 */
class ArquivoTest {

    @Test
    void deveIncrementarContadorEAtualizarUltimaLeituraAteACadaLeitura() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setQuantidadeLeituras(2L);
        Instant primeira = Instant.parse("2023-01-01T00:00:00Z");

        // Acao
        arquivo.registrarLeitura(primeira);

        // Assert
        assertThat(arquivo.getQuantidadeLeituras()).isEqualTo(3L);
        assertThat(arquivo.getUltimaLeitura()).isEqualTo(primeira);
    }

    @Test
    void deveIncrementarContadorACadaChamadaDeRegistarLeitura() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setQuantidadeLeituras(5L);

        // Acao
        arquivo.registrarLeitura(Instant.parse("2023-01-01T00:00:00Z"));
        arquivo.registrarLeitura(Instant.parse("2023-01-02T00:00:00Z"));
        arquivo.registrarLeitura(Instant.parse("2023-01-03T00:00:00Z"));

        // Assert
        assertThat(arquivo.getQuantidadeLeituras()).isEqualTo(8L);
        // A ultima leitura deve ser a mais recente
        assertThat(arquivo.getUltimaLeitura()).isEqualTo(Instant.parse("2023-01-03T00:00:00Z"));
    }

    @Test
    void deveIniciarContadorEmUmQuandoQuantidadeNaoEstabelecida() {
        // Preparacao
        Arquivo arquivo = new Arquivo(); // quantidadeLeituras null

        // Acao
        arquivo.registrarLeitura(Instant.now());

        // Assert
        assertThat(arquivo.getQuantidadeLeituras()).isEqualTo(1L);
        assertThat(arquivo.getUltimaLeitura()).isNotNull();
    }

    @Test
    void deveRegistrarContadorZeroComoPrimeiraLeitura() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setQuantidadeLeituras(0L);

        // Acao
        arquivo.registrarLeitura(Instant.now());

        // Assert
        assertThat(arquivo.getQuantidadeLeituras()).isEqualTo(1L);
    }

    @Test
    void deveExporAtivoEDataDesativacao() {
        // Preparacao
        Arquivo arquivo = new Arquivo();
        arquivo.setAtivo(true);
        Instant dataDesativacao = Instant.parse("2024-01-01T00:00:00Z");
        arquivo.setDataDesativacao(dataDesativacao);

        // Assert
        assertThat(arquivo.isAtivo()).isTrue();
        assertThat(arquivo.getDataDesativacao()).isEqualTo(dataDesativacao);
    }

    @Test
    void devePermitirDesativacaoDeRegistro() {
        // Preparacao: registro ativo sem data de desativacao
        Arquivo arquivo = new Arquivo();
        arquivo.setAtivo(true);

        // Acao: desativacao
        arquivo.setAtivo(false);
        arquivo.setDataDesativacao(Instant.now());

        // Assert
        assertThat(arquivo.isAtivo()).isFalse();
        assertThat(arquivo.getDataDesativacao()).isNotNull();
    }
}
