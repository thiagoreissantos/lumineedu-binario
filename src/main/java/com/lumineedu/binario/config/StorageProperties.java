package com.lumineedu.binario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Objeto de configuracao que mapeia as propriedades do bloco
 * {@code app.storage} do arquivo {@code application.properties}.
 * Controla o diretorio de armazenamento e os limites de tamanho.
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Diretorio raiz onde os arquivos serao gravados no disco. */
    private String diretorio = "./armazenamento";

    /** Subdiretorio dentro do diretorio raiz. */
    private String subdiretorio = "arquivos";

    /** Limite maximo de tamanho do arquivo recebido (em bytes). */
    private long maxTamanhoBytes = 52428800L;

    /** Limite maximo de tamanho permitido pelo disco (em bytes). */
    private long maxTamanhoHdBytes = 1073741824L;

    /**
     * Periodo maximo (em dias) durante o qual um arquivo deve ser acessado
     * (recuperado) para nao ser considerado candidato a limpeza. Arquivos
     * sem acesso (ou com ultima leitura) mais antigos que este periodo
     * passam a ser excluidos pelo Job de limpeza.
     * <p>
     * Valor base por padrao: 730 dias (2 anos).
     */
    private long maxIdleDias = 730L;

    /**
     * Hora (0-23) de execucao do Job de limpeza periodica (1x por dia),
     * no fuso America/Sao_Paulo.
     */
    private int horaExecucaoLimpeza = 3;

    /**
     * Retorna o caminho completo (raiz + subdiretorio) onde os arquivos
     * serao gravados.
     *
     * @return caminho base completo do armazenamento
     */
    public String getDiretorioCompleto() {
        if (diretorio == null || diretorio.isBlank()) {
            return ".";
        }
        return diretorio.endsWith(java.io.File.separator)
                ? diretorio + subdiretorio
                : diretorio + java.io.File.separator + subdiretorio;
    }

    public String getDiretorio() {
        return diretorio;
    }

    public void setDiretorio(String diretorio) {
        this.diretorio = diretorio;
    }

    public String getSubdiretorio() {
        return subdiretorio;
    }

    public void setSubdiretorio(String subdiretorio) {
        this.subdiretorio = subdiretorio;
    }

    public long getMaxTamanhoBytes() {
        return maxTamanhoBytes;
    }

    public void setMaxTamanhoBytes(long maxTamanhoBytes) {
        this.maxTamanhoBytes = maxTamanhoBytes;
    }

    public long getMaxTamanhoHdBytes() {
        return maxTamanhoHdBytes;
    }

    public void setMaxTamanhoHdBytes(long maxTamanhoHdBytes) {
        this.maxTamanhoHdBytes = maxTamanhoHdBytes;
    }

    public long getMaxIdleDias() {
        return maxIdleDias;
    }

    public void setMaxIdleDias(long maxIdleDias) {
        this.maxIdleDias = maxIdleDias;
    }

    public int getHoraExecucaoLimpeza() {
        return horaExecucaoLimpeza;
    }

    public void setHoraExecucaoLimpeza(int horaExecucaoLimpeza) {
        this.horaExecucaoLimpeza = horaExecucaoLimpeza;
    }
}
