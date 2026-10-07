package com.lumineedu.binario.frontend.service;

import org.springframework.stereotype.Service;

import com.lumineedu.binario.frontend.dto.EstatisticasResponse;
import com.lumineedu.binario.frontend.service.web.WebClientService;

/**
 * Camada de negocio da interface (frontend) para obter estatisticas de
 * armazenamento.
 * <p>
 * Encapsula a chamada ao endpoint {@code GET /api/arquivos/estatisticas} e
 * delega a transferencia para o {@link WebClientService}. As Views apenas
 * utilizam esta camada e nunca fazem chamadas HTTP diretamente.
 */
@Service
public class EstatisticaService {

    private final WebClientService webClientService;

    public EstatisticaService(WebClientService webClientService) {
        this.webClientService = webClientService;
    }

    /**
     * Obtém as estatisticas de armazenamento do backend.
     *
     * @return as estatisticas de armazenamento.
     */
    public EstatisticasResponse obter() {
        return webClientService.estatisticas();
    }
}
