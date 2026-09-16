package com.rassini.graphite_client.service.sync.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.rassini.graphite_client.client.GraphiteApiClient;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphiteProfileRefreshServiceImpl implements GraphiteProfileRefreshService {

    private final GraphiteApiClient apiClient;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processAndSaveInternal(String publicId, String detonante) {
        log.info("[FLOW-PHASE-1][DOWNLOAD] supplier={} detonante='{}' - Solicitando perfil a Graphite", publicId, detonante);

        if (publicId == null || publicId.isBlank()) {
            log.error("[FLOW-PHASE-1][DOWNLOAD] supplier={} publicId es nulo o vacio", publicId);
            return false;
        }

        try {

            // IMPORTANTE: false para no aplicar reglas que modifiquen el payload
            JsonNode profile = apiClient.getProfile(publicId, false);

            if (profile == null || profile.isNull() || profile.isMissingNode()) {
                log.error("[FLOW-PHASE-1][DOWNLOAD] supplier={} Perfil vacio o no disponible en Graphite", publicId);
                return false;
            }

            String json = profile.toPrettyString();

            log.info(
                "[FLOW-PHASE-1][DOWNLOAD] supplier={} Perfil recibido exitosamente de Graphite. Tamano JSON={} caracteres",
                publicId,
                json.length()
            );

            SupplierEntity entity = supplierRepository.findById(publicId)
                    .orElseGet(() -> {
                        SupplierEntity e = new SupplierEntity();
                        e.setPublicId(publicId);
                        return e;
                    });

            entity.setStatus(ProviderState.DESCARGA);
            entity.setFullJson(json);
            entity.setLastSync(LocalDateTime.now());

            SupplierEntity saved = supplierRepository.save(entity);

            log.info(
                "[FLOW-PHASE-1][PERSIST] supplier={} Proveedor guardado en BD con status=DESCARGA. JSON almacenado={} caracteres. LastSync={}",
                saved.getPublicId(),
                saved.getFullJson() != null ? saved.getFullJson().length() : 0,
                saved.getLastSync()
            );

            return true;

        } catch (HttpClientErrorException.NotFound e) {

            log.warn("[FLOW-PHASE-1][DOWNLOAD] supplier={} Proveedor no encontrado en Graphite (404)", publicId);
            return false;

        } catch (Exception e) {

            log.error("[FLOW-PHASE-1][DOWNLOAD] supplier={} Error al consultar o guardar perfil desde Graphite: {}",
                    publicId, e.getMessage(), e);
            return false;
        }
    }
}