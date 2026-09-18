package com.lumineedu.binario.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.lumineedu.binario.config.StorageProperties;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.repository.ArquivoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que executa periodicamente o Job de limpeza de arquivos binarios
 * nao acessados ha muito tempo.
 * <p>
 * A cada execucao, identifica os registros ainda ativos cujas ultimas leituras
 * (ou ausencia delas) datam de mais de {@code max-idle-dias} e, para cada um,
 * exclui o conteudo fisico do sistema de arquivos e marca o registro como
 * desativado no banco de metadados. O conteudo fisico e removido de forma
 * resiliente: se o arquivo nao puder ser excluido, o registro permanece ativo
 * e sera reavaliado em uma execucao subsequente.
 */
@Component
public class ArquivoCleanerService {

    private static final Logger log = LoggerFactory.getLogger(ArquivoCleanerService.class);

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorageService arquivoStorageService;
    private final StorageProperties storageProperties;

    public ArquivoCleanerService(ArquivoRepository arquivoRepository,
                                 ArquivoStorageService arquivoStorageService,
                                 StorageProperties storageProperties) {
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorageService = arquivoStorageService;
        this.storageProperties = storageProperties;
    }

    /**
     * Executa o Job de limpeza: localiza os arquivos candidatos (ativos e sem
     * acesso recente), exclui seu conteudo fisico e os desativa no banco.
     * <p>
     * O metodo e transacional: se a persistencia falhar, a exclusao fisica ja
     * realizada e revertida, evitando a perda de conteudo. A exclusao fisica e
     * tolerante a falhas: um arquivo que nao puder ser removido nao impede a
     * continua da execucao e o registro permanece ativo para reavaliacao.
     *
     * @return a lista dos arquivos que foram desativados nesta execucao
     */
    @Scheduled(cron = "0 ${app.storage.hora-execucao-limpeza} * * *")
    @Transactional
    public List<Arquivo> executar() {
        Instant cutoff = Instant.now().minus(storageProperties.getMaxIdleDias(), ChronoUnit.DAYS);
        List<Arquivo> candidatos = arquivoRepository.encontrarCandidatosAPesquisa(cutoff);
        if (candidatos.isEmpty()) {
            log.info("Nenhum arquivo candidato a limpeza.");
            return List.of();
        }

        List<Arquivo> desativados = new ArrayList<>();
        for (Arquivo arquivo : candidatos) {
            if (!eCaminhoFisicoValido(arquivo)) {
                log.warn("Candidato a limpeza sem caminho fisico valido [id={}]; preservado.", arquivo.getId());
                continue;
            }
            try {
                arquivoStorageService.removerFisico(arquivo.getCaminhoFisico());
            } catch (RuntimeException e) {
                log.warn("Falha ao remover o arquivo fisico [id={}]: {}.", arquivo.getId(), e.getMessage());
                continue;
            }
            arquivo.setAtivo(false);
            arquivo.setDataDesativacao(Instant.now());
            desativados.add(arquivo);
        }

        if (desativados.isEmpty()) {
            log.info("Nenhum arquivo fisico removido; nenhum registro desativado.");
        } else {
            log.info("Limpeza concluida: {} arquivo(s) removido(s) e desativado(s).", desativados.size());
            arquivoRepository.saveAll(desativados);
        }
        return desativados;
    }

    /**
     * Verifica se o candidato possui um caminho fisico valido para exclusao.
     *
     * @param arquivo o candidato a limpeza
     * @return true se o caminho fisico e presente e nao vazio
     */
    private boolean eCaminhoFisicoValido(Arquivo arquivo) {
        return arquivo.getCaminhoFisico() != null && !arquivo.getCaminhoFisico().isBlank();
    }
}
