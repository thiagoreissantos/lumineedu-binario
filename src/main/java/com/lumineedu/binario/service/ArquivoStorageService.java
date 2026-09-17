package com.lumineedu.binario.service;

import java.nio.file.Files;
import java.nio.file.Path;

import com.lumineedu.binario.config.SecurityProperties;
import com.lumineedu.binario.config.StorageProperties;
import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.exception.ArquivoInvalidoException;
import com.lumineedu.binario.exception.ArquivoJaExisteException;
import com.lumineedu.binario.exception.ArmazenamentoException;
import com.lumineedu.binario.exception.TamanhoInvalidoException;
import com.lumineedu.binario.util.ArquivoUtil;
import com.lumineedu.binario.util.ImageProcessor;

import org.springframework.stereotype.Service;

/**
 * Servicio responsavel pela operacao fisica dos arquivos: valida os
 * metadados, decodifica o conteudo Base64, processa imagens (redimensionando
 * quando necessario) e grava os arquivos no disco local.
 */
@Service
public class ArquivoStorageService {

    private final StorageProperties storageProperties;
    private final SecurityProperties securityProperties;

    public ArquivoStorageService(StorageProperties storageProperties,
            SecurityProperties securityProperties) {
        this.storageProperties = storageProperties;
        this.securityProperties = securityProperties;
    }

    /**
     * Processa e grava um arquivo no disco local a partir da solicitacao.
     *
     * @param dto a solicitacao de criacao do arquivo
     * @return um objeto com o caminho fisico e o tamanho do arquivo gravado
     */
    public ArquivoArmazenado salvar(ArquivoDTO dto) {
        dto.validar(storageProperties.getMaxTamanhoBytes());

        String nomeOriginal = ArquivoUtil.sanitizarNome(dto.getNomeOriginal());

        // Verifica se o arquivo ja existe no sistema de arquivos
        if (ArquivoUtil.arquivoExiste(caminhoBase(), nomeOriginal)) {
            throw new ArquivoJaExisteException(nomeOriginal);
        }

        byte[] conteudo = ArquivoUtil.decodificarBase64(dto.getConteudoBase64());
        long tamanho = conteudo.length;

        // Verifica se o tipo mime e permitido
        if (!eTipoMimePermitido(dto.getTipoMime())) {
            throw new ArquivoInvalidoException(
                    "tipo mime nao permitido. Tipos permitidos: "
                            + securityProperties.getMimeTiposPermitidos());
        }

        // Processa a imagem: se for imagem e exceder o limite do HD, redimensiona mantendo as proporcoes
        if (eImagem(conteudo)) {
            byte[] conteudoFinal = conteudo;
            int[] dimensoes = ImageProcessor.getDimensoes(conteudo);
            if (dimensoes != null && Math.max(dimensoes[0], dimensoes[1]) > storageProperties.getMaxTamanhoHdBytes()) {
                conteudoFinal = ImageProcessor.redimensionar(conteudo,
                        (int) storageProperties.getMaxTamanhoHdBytes());
            }
            if (!eImagem(conteudoFinal)) {
                throw new ArquivoInvalidoException("conteudo nao e uma imagem valida apos processamento");
            }
            conteudo = conteudoFinal;
        }

        String nomeArquivo = gerarNomeArquivoUnico(nomeOriginal);
        String caminhoFisico = construirCaminhoCompleto(nomeArquivo);
        ArquivoUtil.gravarArquivo(caminhoFisico, conteudo);

        return new ArquivoArmazenado(nomeArquivo, caminhoFisico, tamanho);
    }

    /**
     * Retorna o caminho base (raiz + subdiretorio) do armazenamento.
     *
     * @return o caminho base do armazenamento
     */
    public String caminhoBase() {
        return storageProperties.getDiretorioCompleto();
    }

    /**
     * Constrói o caminho fisico completo de um arquivo.
     *
     * @param nomeArquivo nome do arquivo
     * @return o caminho fisico completo
     */
    public String construirCaminhoCompleto(String nomeArquivo) {
        return caminhoBase() + java.io.File.separator + nomeArquivo;
    }

    /**
     * Gera um nome de arquivo unico dentro do diretorio de armazenamento.
     *
     * @param nomeOriginal nome original do arquivo
     * @return o nome de arquivo unico
     */
    public String gerarNomeArquivoUnico(String nomeOriginal) {
        return ArquivoUtil.gerarNomeUnico(caminhoBase(), nomeOriginal);
    }

    /**
     * Verifica se um conteudo binario representa uma imagem.
     *
     * @param conteudoBytes o conteudo binario
     * @return true se e uma imagem
     */
    public boolean eImagem(byte[] conteudoBytes) {
        return ImageProcessor.eImagemValida(conteudoBytes);
    }

    /**
     * Verifica se um tipo mime e permitido.
     *
     * @param tipoMime o tipo mime a ser verificado
     * @return true se o tipo mime e permitido
     */
    public boolean eTipoMimePermitido(String tipoMime) {
        return securityProperties.getMimeTiposPermitidos().contains(tipoMime.toLowerCase());
    }

    /**
     * Limpa e remove um arquivo do disco local.
     *
     * @param caminhoFisico caminho completo do arquivo a ser removido
     * @throws ArmazenamentoException em caso de erro
     */
    public void removerFisico(String caminhoFisico) {
        Path caminho = Path.of(caminhoFisico);
        if (Files.exists(caminho)) {
            try {
                Files.deleteIfExists(caminho);
            } catch (Exception e) {
                throw new ArmazenamentoException("erro ao remover o arquivo do disco: " + e.getMessage());
            }
        }
    }

    /**
     * Classe auxiliar que guarda o resultado da operacao de armazenamento.
     */
    public static final class ArquivoArmazenado {

        private final String nomeArquivo;
        private final String caminhoFisico;
        private final long tamanhoBytes;
        private final String caminhoRelativo;

        public ArquivoArmazenado(String nomeArquivo, String caminhoFisico, long tamanhoBytes) {
            this.nomeArquivo = nomeArquivo;
            this.caminhoFisico = caminhoFisico;
            this.tamanhoBytes = tamanhoBytes;
            this.caminhoRelativo = "arquivos/" + nomeArquivo;
        }

        public String getNomeArquivo() {
            return nomeArquivo;
        }

        public String getCaminhoFisico() {
            return caminhoFisico;
        }

        public long getTamanhoBytes() {
            return tamanhoBytes;
        }

        public String getCaminhoRelativo() {
            return caminhoRelativo;
        }
    }
}
