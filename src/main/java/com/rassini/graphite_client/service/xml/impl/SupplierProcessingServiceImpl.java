package com.rassini.graphite_client.service.xml.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.SupplierRepository;
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
public class SupplierProcessingServiceImpl
        implements SupplierProcessingService {

    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    private final SupplierJpaMapper supplierJpaMapper;
    private final XmlOcService xmlOcService;
    private final XmlPnService xmlPnService;
    private final XmlFrenosService xmlFrenosService;
    private final XmlBreakesService xmlBreakesService;
    private final IntegrityService integrityService;

    @Override
    public void processSupplier(String publicId) {

        //  Tomar solo si está en DESCARGA
        SupplierEntity supplier = supplierRepository
            .findByPublicIdAndStatus(publicId, ProviderState.DESCARGA)
            .orElse(null);

        if (supplier == null) {
            // No está en DESCARGA → no se procesa
            return;
        }

        try {
            //  PROCESSINGJPA
            updateStatus(supplier, ProviderState.PROCESSINGJPA);

            GraphiteSupplierDto dto =
                objectMapper.readValue(supplier.getFullJson(),
                                        GraphiteSupplierDto.class);

            // Inserta en tabla suppliers (una fila por ERP_Record)
            
            log.info("[PROCESS] Antes de upsertSuppliersRows");
            supplierJpaMapper.upsertSuppliersRows(dto);
            log.info("[PROCESS] Despues de upsertSuppliersRows");

            
            log.info(
                "[PROCESS] ERPs en dto: {}",
                dto.getErpRecords() == null
                    ? "null"
                    : dto.getErpRecords()
                        .stream()
                        .map(e -> "'" + e.getRassiniErpEntityId() + "'")
                        .toList()
            );


            // 3 XML OC (0111)
            updateStatus(supplier, ProviderState.PROCESSINGXMLOC);
            xmlOcService.generate(dto);

            // 4️ XML PN (09)
            updateStatus(supplier, ProviderState.PROCESSINGXMLPN);
            xmlPnService.generate(dto);

            // 5️ XML Frenos (1000)
            updateStatus(supplier, ProviderState.PROCESSINGXMLFRN);
            xmlFrenosService.generate(dto);

            // 6 XML Breakes (1850)
            updateStatus(supplier, ProviderState.PROCESSINGXMLBRK);
            xmlBreakesService.generate(dto);

            // 7 Todos los XML listos
            updateStatus(supplier, ProviderState.PROCESSINGXMLCOMPLETE);


            //genera archivo de integrity
            integrityService.createFileSupplierSync(dto.getErpIdQad());


           

        } catch (Exception e) {
            // ❗ No cambias status (no hay ERROR en enum)
            // Queda en el último estado alcanzado
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