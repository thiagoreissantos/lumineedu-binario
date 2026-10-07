package com.lumineedu.binario.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.lumineedu.binario.dto.ArquivoDTO;
import com.lumineedu.binario.dto.ArquivoResponse;
import com.lumineedu.binario.entity.Arquivo;
import com.lumineedu.binario.util.ArquivoUtil;
import com.lumineedu.binario.exception.ArquivoNaoEncontradoException;
import com.lumineedu.binario.repository.ArquivoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio principal para administracao de arquivos binarios.
 * Orquestra a persistencia (repository) e a operacao fisica (storage).
 */
@Service
@Transactional
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorageService arquivoStorageService;

    public ArquivoService(ArquivoRepository arquivoRepository,
            ArquivoStorageService arquivoStorageService) {
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorageService = arquivoStorageService;
    }

    /**
     * Cria um arquivo binario: valida e processa o conteudo, grava no disco
     * e persiste os metadados no banco de dados.
     *
     * @param dto a solicitacao de criacao
     * @return a resposta do arquivo criado
     */
    public ArquivoResponse criar(ArquivoDTO dto) {
        byte[] conteudo = dto.getConteudo();
        // Hash do conteudo binario (SHA-256) para a deduplicacao por conteudo.
        String hash = ArquivoUtil.calcularHash(conteudo);

        // Deduplicacao por conteudo binario: se ja existe um registro ATIVO com
        // o mesmo conteudo, reutiliza o caminho fisico ja armazenado (nao
        // reescreve o conteudo nem cria um arquivo duplicado no disco) e
        // persiste um novo registro que aponta para o mesmo arquivo fisico.
        // Apenas um registro ATIVO e reutilizado: um arquivo inativo (arquivo
        // fisico ja removido pelo Job de limpeza) nao pode reutilizar seu
        // caminho, logo o conteudo deve ser gravado novamente.
        Arquivo existenteAtivo = arquivoRepository
                .findByHash(hash)
                .filter(Arquivo::isAtivo)
                .orElse(null);

        if (existenteAtivo != null) {
            Arquivo anterior = existenteAtivo;
            Arquivo arquivo = new Arquivo();
            arquivo.copiarDe(dto);
            arquivo.setCaminhoRelativo(anterior.getCaminhoRelativo());
            arquivo.setCaminhoFisico(anterior.getCaminhoFisico());
            arquivo.setTamanhoBytes(anterior.getTamanhoBytes());
            arquivo.setHash(hash);
            arquivo.setDataCriacao(anterior.getDataCriacao());
            arquivo.setQuantidadeLeituras(0L);
            // Novo registro entra como ativo; passa a ser inativo apenas apos o
            // Job de limpeza excluir o conteudo fisico (se nao acessado).
            arquivo.setAtivo(true);
            arquivo.setDataAtualizacao(Instant.now());

            Arquivo salvo = arquivoRepository.save(arquivo);
            return ArquivoResponse.de(salvo);
        }

        // Sem duplicado: o storage (nao mais redimensiona) grava o conteudo no
        // disco e o novo registro aponta para o arquivo gravado.
        ArquivoStorageService.ArquivoArmazenado armazenado = arquivoStorageService.salvar(dto);

        Arquivo arquivo = new Arquivo();
        arquivo.copiarDe(dto);
        arquivo.definirCaminhoEConteudo(
                armazenado.getCaminhoRelativo(),
                armazenado.getCaminhoFisico(),
                armazenado.getTamanhoBytes());
        arquivo.setHash(hash);
        arquivo.setQuantidadeLeituras(0L);
        // Novo registro entra como ativo; passa a ser inativo apenas apos o
        // Job de limpeza excluir o conteudo fisico (se nao acessado).
        arquivo.setAtivo(true);

        Arquivo salvo = arquivoRepository.save(arquivo);
        return ArquivoResponse.de(salvo);
    }

    /**
     * Busca um arquivo por seu identificador.
     *
     * @param id identificador do arquivo
     * @return a resposta do arquivo encontrado
     * @throws ArquivoNaoEncontradoException se o arquivo nao existe
     */
    @Transactional
    public ArquivoResponse buscarPorId(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(ArquivoNaoEncontradoException::new);
        if (!arquivo.isAtivo()) {
            throw new ArquivoNaoEncontradoException();
        }
        registrarLeitura(arquivo);
        return ArquivoResponse.de(arquivo);
    }

    /**
     * Registra uma leitura (recuperacao) do arquivo: atualiza o timestamp
     * da ultima leitura e incrementa o contador de leituras, persistindo os
     * metadados atualizados no banco de dados.
     *
     * @param arquivo a entidade a ser atualizada
     */
    private void registrarLeitura(Arquivo arquivo) {
        Instant agora = Instant.now();
        arquivo.registrarLeitura(agora);
        arquivoRepository.save(arquivo);
    }

    /**
     * Listagem paginada de todos os arquivos.
     *
     * @param pageable a paginacao
     * @return a pagina de respostas de arquivos
     */
    @Transactional(readOnly = true)
    public Page<ArquivoResponse> listar(Pageable pageable) {
        Page<Arquivo> pagina = arquivoRepository.findAll(pageable);
        return pagina.map(ArquivoResponse::de);
    }

    /**
     * Busca um arquivo por seu nome original.
     *
     * @param nomeOriginal nome original do arquivo
     * @return a resposta do arquivo encontrado
     * @throws ArquivoNaoEncontradoException se o arquivo nao existe
     */
    @Transactional(readOnly = true)
    public ArquivoResponse buscarPorNome(String nomeOriginal) {
        List<Arquivo> resultados = arquivoRepository.findByNomeOriginal(nomeOriginal);
        if (resultados.isEmpty()) {
            throw new ArquivoNaoEncontradoException();
        }
        return ArquivoResponse.de(resultados.get(0));
    }

    /**
     * Lista os arquivos de um determinado tipo mime.
     *
     * @param tipoMime o tipo mime dos arquivos
     * @return a lista de respostas de arquivos
     */
    @Transactional(readOnly = true)
    public List<ArquivoResponse> listarPorTipoMime(String tipoMime) {
        List<Arquivo> resultados = arquivoRepository.findByTipoMime(tipoMime);
        return resultados.stream().map(ArquivoResponse::de).toList();
    }

    /**
     * Remove um arquivo do sistema: exclui o arquivo fisico e remove os
     * metadados do banco de dados.
     *
     * @param id identificador do arquivo a ser removido
     * @return a resposta indicando sucesso da remocao
     */
    @Transactional
    public ArquivoResponse remover(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(ArquivoNaoEncontradoException::new);

        if (arquivo.getCaminhoFisico() != null && !arquivo.getCaminhoFisico().isBlank()) {
            arquivoStorageService.removerFisico(arquivo.getCaminhoFisico());
        }

        arquivoRepository.delete(arquivo);
        return new ArquivoResponse(id, null, null, null, null, null, null, null, null, null, null, null, false, null, null);
    }

    /**
     * Retorna a quantidade total de arquivos armazenados.
     *
     * @return o total de arquivos
     */
    @Transactional(readOnly = true)
    public long contar() {
        return arquivoRepository.count();
    }

    /**
     * Retorna a soma do tamanho (em bytes) de todos os arquivos ainda ativos.
     * Arquivos inativos (desativados pelo Job de limpeza) nao entram na soma.
     *
     * @return a soma do tamanho dos arquivos ativos, ou zero quando nao ha nenhum
     */
    @Transactional(readOnly = true)
    public long totalBytesAtivos() {
        Long total = arquivoRepository.totalBytesAtivos();
        return total == null ? 0L : total;
    }
}
