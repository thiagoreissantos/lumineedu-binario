package com.lumineedu.binario.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArmazenamentoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;

import org.springframework.util.StringUtils;

/**
 * Conjunto de utilitarios auxiliares para manipulacao de caminhos,
 * nomes de arquivos, codificacao Base64 e gravacao no disco local.
 */
public final class ArquivoUtil {

    private ArquivoUtil() {
    }

    /**
     * Sanitiza um nome de arquivo, removendo caracteres perigosos
     * (caminho, caracteres de controle, etc) e garantindo que o nome
     * resultante seja unico dentro do diretorio de armazenamento.
     *
     * @param nomeOriginal o nome original do arquivo
     * @return o nome de arquivo seguro e unico
     */
    public static String sanitizarNome(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new ArquivoInvalidoException("nome do arquivo nao pode ser vazio");
        }
        String nome = nomeOriginal.trim();
        // Remove qualquer sequencia de separador de caminho
        nome = nome.replaceAll("[\\\\/]+", "_");
        // Remove caracteres de controle nao imprimiveis
        nome = nome.replaceAll("[\\x00-\\x20\\x7F-\\xFF]", "");
        // Evita nomes vazios ou que so contenham pontos
        nome = nome.replaceAll("(^\\.*|\\.+$)", "");
        if (nome.isEmpty()) {
            throw new ArquivoInvalidoException("nome do arquivo invalido");
        }
        return nome;
    }

    /**
     * Verifica se ja existe um arquivo com o mesmo nome no diretorio de
     * armazenamento.
     *
     * @param caminhoBase  caminho base do armazenamento
     * @param nomeArquivo  nome do arquivo a ser verificado
     * @return true se o arquivo ja existe
     */
    public static boolean arquivoExiste(String caminhoBase, String nomeArquivo) {
        return new File(caminhoBase, nomeArquivo).exists();
    }

    /**
     * Gera um nome de arquivo unico adicionando um sufixo numerico.
     *
     * @param caminhoBase   caminho base do armazenamento
     * @param nomeArquivo   nome do arquivo a ser tornado unico
     * @return o nome de arquivo unico
     */
    public static String gerarNomeUnico(String caminhoBase, String nomeArquivo) {
        int indice = 1;
        String candidato = nomeArquivo;
        while (arquivoExiste(caminhoBase, candidato)) {
            int ultimoPonto = candidato.lastIndexOf('.');
            String nome = ultimoPonto > 0 ? candidato.substring(0, ultimoPonto) : candidato;
            String extensao = ultimoPonto > 0 ? candidato.substring(ultimoPonto) : "";
            candidato = String.format("%s_%d%s", nome, indice, extensao);
            indice++;
        }
        return candidato;
    }

    /**
     * Constrói o caminho fisico completo de um arquivo dentro do
     * diretorio de armazenamento.
     *
     * @param caminhoBase  caminho base do armazenamento
     * @param nomeArquivo  nome do arquivo a ser gravado
     * @return o caminho fisico completo
     */
    public static String construirCaminho(String caminhoBase, String nomeArquivo) {
        Path base = Path.of(caminhoBase);
        Path caminho = base.resolve(nomeArquivo).normalize();
        String caminhoFisico = caminho.toString();
        String caminhoRelativo = base.relativize(caminho).toString();
        return new String(caminhoRelativo.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    /**
     * Garante que o diretorio de armazenamento exista, criando-o se
     * necessario.
     *
     * @param caminhoBase o diretorio que deve ser criado
     * @throws ArmazenamentoException em caso de erro
     */
    public static void garantirDiretorio(String caminhoBase) {
        try {
            Path diretorio = Path.of(caminhoBase);
            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
            }
        } catch (IOException e) {
            throw new ArmazenamentoException("nao foi possivel criar o diretorio de armazenamento: "
                    + e.getMessage());
        }
    }

    /**
     * Grava o conteudo binario em disco no caminho especificado.
     *
     * @param caminhoFisico caminho completo onde o arquivo sera gravado
     * @param conteudoBytes o conteudo binario a ser gravado
     * @throws ArmazenamentoException em caso de erro de escrita
     */
    public static void gravarArquivo(String caminhoFisico, byte[] conteudoBytes) {
        try {
            Path diretorio = Path.of(caminhoFisico).getParent();
            if (diretorio != null && !Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
            }
            Files.write(Path.of(caminhoFisico), conteudoBytes);
        } catch (IOException e) {
            throw new ArmazenamentoException("erro ao gravar o arquivo no disco: " + e.getMessage());
        }
    }

    /**
     * Verifica se um conteudo Base64 e valido e retorna os bytes
     * decodificados.
     *
     * @param conteudoBase64 a string Base64 a ser decodificada
     * @return o conteudo binario decodificado
     * @throws ArquivoInvalidoException se o conteudo nao for Base64 valido
     */
    public static byte[] decodificarBase64(String conteudoBase64) {
        if (conteudoBase64 == null || conteudoBase64.isBlank()) {
            throw new ArquivoInvalidoException("conteudoBase64 nao pode ser vazio");
        }
        try {
            return Base64.getDecoder().decode(conteudoBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new ArquivoInvalidoException("conteudoBase64 invalido: " + e.getMessage());
        }
    }

    /**
     * Codifica um array de bytes em uma string Base64.
     *
     * @param conteudoBytes o conteudo binario a ser codificado
     * @return a string Base64 codificada
     */
    public static String codificarBase64(byte[] conteudoBytes) {
        return Base64.getEncoder().encodeToString(conteudoBytes);
    }

    /**
     * Verifica se o tamanho do conteudo excede o limite configurado.
     *
     * @param tamanhoBytes tamanho em bytes
     * @param limiteBytes  limite maximo em bytes
     * @throws TamanhoInvalidoException se o tamanho exceder o limite
     */
    public static void validarTamanho(long tamanhoBytes, long limiteBytes) {
        if (tamanhoBytes > limiteBytes) {
            throw new TamanhoInvalidoException(
                    String.format("arquivo excede o limite maximo de %d bytes", limiteBytes));
        }
    }
}
