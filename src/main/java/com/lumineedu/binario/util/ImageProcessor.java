package com.lumineedu.binario.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.awt.RenderingHints;

import javax.imageio.ImageIO;

import com.lumineedu.binario.exception.ArquivoInvalidoException;

/**
 * Utilitario responsavel por processar imagens recebidas no upload.
 * Verifica se o conteudo e uma imagem valida e, caso ela ultrapasse
 * o limite de resolucao configurado, redimensiona-a mantendo as
 * proporcoes originais.
 */
public final class ImageProcessor {

    private ImageProcessor() {
    }

    /**
     * Verifica se o conteudo binario e uma imagem valida.
     *
     * @param conteudoBytes o conteudo binario a ser verificado
     * @return true se e uma imagem valida
     */
    public static boolean eImagemValida(byte[] conteudoBytes) {
        if (conteudoBytes == null || conteudoBytes.length == 0) {
            return false;
        }
        try (ByteArrayInputStream entrada = new ByteArrayInputStream(conteudoBytes)) {
            BufferedImage imagem = ImageIO.read(entrada);
            return imagem != null;
        } catch (IOException e) {
            throw new ArquivoInvalidoException("conteudo nao e uma imagem valida: " + e.getMessage());
        }
    }

    /**
     * Redimensiona uma imagem mantendo as proporcoes, caso ela
     * ultrapasse a resolucao maxima configurada.
     *
     * @param conteudo o conteudo binario da imagem
     * @param limiteMaxLado     lado maximo (largura e altura) em pixels
     * @return o novo conteudo binario da imagem (possivelmente redimensionada)
     */
    public static byte[] redimensionar(byte[] conteudo, int limiteMaxLado) {
        BufferedImage imagemOriginal;
        try (ByteArrayInputStream entrada = new ByteArrayInputStream(conteudo)) {
            imagemOriginal = ImageIO.read(entrada);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("conteudo nao e uma imagem valida: " + e.getMessage());
        }

        if (imagemOriginal == null) {
            throw new ArquivoInvalidoException("conteudo nao e uma imagem valida");
        }

        int largura = imagemOriginal.getWidth();
        int altura = imagemOriginal.getHeight();

        boolean precisaRedimensionar = largura > limiteMaxLado || altura > limiteMaxLado;
        if (!precisaRedimensionar) {
            return conteudo;
        }

        // Calcula a escala mantendo a proporcao da imagem original
        double escala = Math.min((double) limiteMaxLado / largura, (double) limiteMaxLado / altura);
        int novaLargura = (int) Math.max(1, Math.round(largura * escala));
        int novaAltura = (int) Math.max(1, Math.round(altura * escala));

        BufferedImage imagemRedimensionada = new BufferedImage(novaLargura, novaAltura,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = imagemRedimensionada.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(imagemOriginal, 0, 0, novaLargura, novaAltura, null);
        graphics.dispose();

        try {
            return codificar(imagemRedimensionada);
        } catch (IOException exception) {
            throw new ArquivoInvalidoException("conteudo nao pode ser processado: " + exception.getMessage());
        }
    }

    /**
     * Codifica uma imagem em buffer para bytes binarios.
     *
     * @param imagem a imagem a ser codificada
     * @return o conteudo binario codificado
     * @throws IOException em caso de erro de codificacao
     */
    public static byte[] codificar(BufferedImage imagem) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return saida.toByteArray();
    }

    /**
     * Extrai as dimensoes (largura x altura) de um conteudo binario de imagem.
     *
     * @param conteudoBytes o conteudo binario da imagem
     * @return um array {@code [largura, altura]} ou {@code null} se nao for imagem
     */
    public static int[] getDimensoes(byte[] conteudoBytes) {
        BufferedImage imagem;
        try (ByteArrayInputStream entrada = new ByteArrayInputStream(conteudoBytes)) {
            imagem = ImageIO.read(entrada);
        } catch (IOException e) {
            return null;
        }
        if (imagem == null) {
            return null;
        }
        return new int[] { imagem.getWidth(), imagem.getHeight() };
    }

    /**
     * Retorna a cor de fundo utilizada para preencher imagens com transparencia.
     *
     * @return a cor de fundo padrao
     */
    public static Color corFundoPadrao() {
        return new Color(255, 255, 255);
    }
}
