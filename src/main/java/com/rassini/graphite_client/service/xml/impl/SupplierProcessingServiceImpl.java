package com.rassini.graphite_client.service.xml.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.xml.SupplierJpaMapper;
import com.rassini.graphite_client.service.xml.SupplierProcessingService;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlPnService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SupplierProcessingServiceImpl implements SupplierProcessingService {

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    private final SupplierJpaMapper supplierJpaMapper;
    private final XmlOcService xmlOcService;
    private final XmlPnService xmlPnService;
    private final XmlFrenosService xmlFrenosService;
    private final XmlBreakesService xmlBreakesService;
    private final IntegrityService integrityService;

    private final GraphiteProfileRefreshService graphiteProfileRefreshService;


    
    @Override
    public void processSupplier(String publicId, String detonante) {

        // 1. Refrescar desde Graphite y guardar fullJson nuevo
        boolean refreshed = graphiteProfileRefreshService.processAndSaveInternal(publicId,detonante);
        if (!refreshed) {
            return;
        }

        // 2. Tomar nuevamente desde BD solo si quedó en DESCARGA
        SupplierEntity supplier = supplierRepository
            .findByPublicIdAndStatus(publicId, ProviderState.DESCARGA)
            .orElse(null);

        if (supplier == null) {
            return;
        }

        try {
            updateStatus(supplier, ProviderState.PROCESSINGJPA);

            String raw = supplier.getFullJson();

            JsonNode root = objectMapper.readTree(raw);

            
            log.debug("ERP_ID root = {}", root.path("ERP_ID").asText(null));
            log.debug("Entity_Public_Id root = {}", root.path("Entity_Public_Id").asText(null));
            log.debug("Entity_Name root = {}", root.path("Entity_Name").asText(null));

            GraphiteSupplierDto dto =
                objectMapper.readValue(raw, GraphiteSupplierDto.class);

            log.debug("ERP Records: " +
            (dto.getErpRecords() == null ? 0 : dto.getErpRecords().size()));

            log.debug("[PROCESS] Antes de upsertSuppliersRows GraphiteSupplierDto: {}", dto);
            log.debug("[PROCESS] Antes de upsertSuppliersRows");
            supplierJpaMapper.upsertSuppliersRows(dto);
            log.debug("[PROCESS] Despues de upsertSuppliersRows");

            log.debug(
                "[PROCESS] ERPs en dto: {}",
                dto.getErpRecords() == null
                    ? "null"
                    : dto.getErpRecords()
                        .stream()
                        .map(e -> "'" + e.getRassiniErpEntityId() + "'")
                        .toList()
            );

            updateStatus(supplier, ProviderState.PROCESSINGXMLOC);
            xmlOcService.generate(dto);

            updateStatus(supplier, ProviderState.PROCESSINGXMLPN);
            xmlPnService.generate(dto);

            updateStatus(supplier, ProviderState.PROCESSINGXMLFRN);
            xmlFrenosService.generate(dto);

            updateStatus(supplier, ProviderState.PROCESSINGXMLBRK);
            xmlBreakesService.generate(dto);

            updateStatus(supplier, ProviderState.PROCESSINGXMLCOMPLETE);

            integrityService.createFileSupplierSync(dto.getErpIdQad());

        } catch (Exception e) {
            throw new IllegalStateException(
                "Error procesando proveedor " + publicId, e
            );
        }
    }

    private void updateStatus(
            SupplierEntity supplier,
            ProviderState newStatus
    ) {
        supplier.setStatus(newStatus);
        supplier.setLastSync(LocalDateTime.now());
        supplierRepository.save(supplier);
    }
}