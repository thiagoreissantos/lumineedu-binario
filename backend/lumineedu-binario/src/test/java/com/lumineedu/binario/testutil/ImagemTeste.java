package com.lumineedu.binario.testutil;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

/**
 * Utilitario de teste para gerar imagens binarias em memoria,
 * codificadas como Base64, para uso nos testes.
 */
public final class ImagemTeste {

    private ImagemTeste() {
    }

    /**
     * Gera uma imagem PNG pequena com cor preta.
     *
     * @return o conteudo binario da imagem
     */
    public static byte[] gerarPng() {
        return gerar(20, 20, Color.BLACK, "png");
    }

    /**
     * Gera uma imagem JPG de dimensoes arbitrariedades com uma cor.
     *
     * @param largura largura da imagem em pixels
     * @param altura  altura da imagem em pixels
     * @param cor     a cor da imagem
     * @return o conteudo binario da imagem
     */
    public static byte[] gerar(int largura, int altura, Color cor, String formato) {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        imagem.createGraphics().setColor(Color.WHITE);
        imagem.getGraphics().setColor(cor);
        imagem.getGraphics().fillRect(0, 0, largura, altura);
        imagem.getGraphics().dispose();

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagem, formato, saida);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar imagem de teste", e);
        }
        return saida.toByteArray();
    }

    /**
     * Gera uma imagem codificada em Base64 com um nome de arquivo.
     *
     * @param nomeOriginal nome original do arquivo
     * @param largura      largura da imagem em pixels
     * @param altura       altura da imagem em pixels
     * @param cor           a cor da imagem
     * @param formato       formato da imagem (png, jpg, etc)
     * @return um objeto contendo o nome, o tipo mime e o conteudo Base64
     */
    public static ImagemCriacao criarBase64(String nomeOriginal, int largura, int altura, Color cor,
            String formato) {
        byte[] conteudo = gerar(largura, altura, cor, formato);
        String contentType = "image/" + formato;
        return new ImagemCriacao(nomeOriginal, contentType,
                java.util.Base64.getEncoder().encodeToString(conteudo),
                conteudo.length);
    }

    /**
     * Classe auxiliar que guarda os campos de uma imagem Base64 de teste.
     */
    public static final class ImagemCriacao {

        public final String nomeOriginal;
        public final String tipoMime;
        public final String conteudoBase64;
        public final long tamanhoBytes;

        public ImagemCriacao(String nomeOriginal, String tipoMime, String conteudoBase64, long tamanhoBytes) {
            this.nomeOriginal = nomeOriginal;
            this.tipoMime = tipoMime;
            this.conteudoBase64 = conteudoBase64;
            this.tamanhoBytes = tamanhoBytes;
        }
    }
}
