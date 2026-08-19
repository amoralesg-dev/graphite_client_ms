package com.rassini.graphite_client.service.xml.impl;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.sync.GraphiteProfileRefreshService;
import com.rassini.graphite_client.service.sync.IntegrityService;
import com.rassini.graphite_client.service.xml.SupplierJpaMapper;
import com.rassini.graphite_client.service.xml.SupplierProcessingService;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlPn99Service;
import com.rassini.graphite_client.service.xml.XmlPnService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SupplierProcessingServiceImpl implements SupplierProcessingService {


    @Value("${mail.nuevos-proveedores.destinatarios}")
    private String destinatariosNuevoProveedor;

    @Value("${spring.profiles.active:local}")
    private String environment;

    private final SupplierRepository supplierRepository;
    private final CorreoPendienteRepository correoPendienteRepository;
    private final ObjectMapper objectMapper;

    private final SupplierJpaMapper supplierJpaMapper;
    private final XmlOcService xmlOcService;
    private final XmlPnService xmlPnService;
    private final XmlPn99Service xmlPn99Service;
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

            boolean proveedorRecienDescargado = ProviderState.DESCARGA.equals(supplier.getStatus());
            updateStatus(supplier, ProviderState.PROCESSINGJPA);

            String raw = supplier.getFullJson();

            JsonNode root = objectMapper.readTree(raw);

            
            log.debug("ERP_ID root = {}", root.path("ERP_ID").asText(null));
            log.debug("Entity_Public_Id root = {}", root.path("Entity_Public_Id").asText(null));
            log.debug("Entity_Name root = {}", root.path("Entity_Name").asText(null));
            logGraphiteContractIssues(root, supplier.getPublicId());
            GraphiteSupplierDto dto =
                objectMapper.readValue(raw, GraphiteSupplierDto.class);

            log.debug("ERP Records: " +
            (dto.getErpRecords() == null ? 0 : dto.getErpRecords().size()));

            //log.debug("[PROCESS] Antes de upsertSuppliersRows GraphiteSupplierDto: {}", dto);
            //log.debug("[PROCESS] Antes de upsertSuppliersRows");
            supplierJpaMapper.upsertSuppliersRows(dto);
            //log.debug("[PROCESS] Despues de upsertSuppliersRows");

            log.debug(
                "[PROCESS] ERPs en dto: {}",
                dto.getErpRecords() == null
                    ? "null"
                    : dto.getErpRecords()
                        .stream()
                        .map(e -> "'" + e.getRassiniErpEntityId() + "'")
                        .toList()
            );
            if (proveedorRecienDescargado) {
                notificarNuevoProveedor(
                        dto,
                        destinatariosNuevoProveedor,
                        environment
                );
            }


            xmlOcService.generate(dto, supplier);
            if(!ProviderState.ERRORMAPOC.equals(supplier.getStatus()))
                updateStatus(supplier, ProviderState.PROCESSINGXMLOC);
            
            xmlPnService.generate(dto, supplier);
            if(!ProviderState.ERRORMAPPN.equals(supplier.getStatus()))
                updateStatus(supplier, ProviderState.PROCESSINGXMLPN);
            
            xmlPn99Service.generate(dto, supplier);
            if(!ProviderState.ERRORMAPPN.equals(supplier.getStatus()))
                updateStatus(supplier, ProviderState.PROCESSINGXMLPN);
            
            xmlFrenosService.generate(dto, supplier);
            if(!ProviderState.ERRORMAPFRENOS.equals(supplier.getStatus()))
                updateStatus(supplier, ProviderState.PROCESSINGXMLFRN);
            
            xmlBreakesService.generate(dto, supplier);
            if(!ProviderState.ERRORMAPBREAKES.equals(supplier.getStatus()))
                updateStatus(supplier, ProviderState.PROCESSINGXMLBRK);
            
            
            if (!ProviderState.ERRORMAPOC.equals(supplier.getStatus())
                && !ProviderState.ERRORMAPPN.equals(supplier.getStatus())
                && !ProviderState.ERRORMAPFRENOS.equals(supplier.getStatus())
                && !ProviderState.ERRORMAPBREAKES.equals(supplier.getStatus())
                && !ProviderState.ERRORMAPBYPASA.equals(supplier.getStatus())
                && !ProviderState.ERRORMAPPING.equals(supplier.getStatus())) {

                updateStatus(supplier, ProviderState.PROCESSINGXMLCOMPLETE);
                integrityService.createFileSupplierSync(dto.getErpIdQad());
            }

                

        } catch (InvalidFormatException e) {

            log.error(
                    "[GRAPHITE_JSON_ERROR] supplier={} path={} value={} targetType={}",
                    supplier.getPublicId(),
                    e.getPathReference(),
                    e.getValue(),
                    e.getTargetType(),
                    e
            );
            if (e.getPathReference() != null
                    && e.getPathReference().contains("Bank_Number")) {

                log.error(
                        "[GRAPHITE_CONTRACT] supplier={} recibió Bank_Number inválido. Valor='{}'",
                        supplier.getPublicId(),
                        e.getValue()
                );
            }

            throw new IllegalStateException(
                    "Error procesando proveedor " + supplier.getPublicId(),
                    e
            );

        } catch (Exception e) {

            log.error(
                    "Unexpected error processing supplier={}",
                    supplier.getPublicId(),
                    e
            );

            throw new IllegalStateException(
                    "Error procesando proveedor " + supplier.getPublicId(),
                    e
            );
        }
    }

    private void logGraphiteContractIssues(
        JsonNode root,
        String supplierCode) {

        JsonNode erps = root.path("ERP_Record");

        if (!erps.isArray()) {
            return;
        }

        for (JsonNode erp : erps) {

            String bu =
                    erp.path("RASSINI_ERP_Entity_ID").asText();

            JsonNode banks = erp.path("ERP_Bank_List");

            if (!banks.isArray()) {
                continue;
            }

            for (JsonNode bank : banks) {

                JsonNode bankNumberNode =
                        bank.get("Bank_Number");

                if (bankNumberNode != null
                        && bankNumberNode.isTextual()
                        && bankNumberNode.asText().isBlank()) {

                    log.warn(
                            "[GRAPHITE_CONTRACT] supplier={} bu={} account={} Bank_Number llegó como string vacío",
                            supplierCode,
                            bu,
                            bank.path("Bank_Account_Number").asText(),
                            bankNumberNode.asText()
                    );
                }
            }
        }
    }


    private void notificarNuevoProveedor(
        GraphiteSupplierDto dto,
        String destinatariosConfig,
        String environment
    ) {

        String erps = dto.getErpRecords() == null
                ? ""
                : dto.getErpRecords()
                        .stream()
                        .map(GraphiteSupplierDto.ErpRecord::getRassiniErpEntityId)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));

        CorreoPendienteEntity correo = new CorreoPendienteEntity();

        correo.setTo(destinatariosConfig);

        correo.setSubject(
                "Nuevo proveedor recibido desde Graphite "
                + dto.getEntityPublicId()
                + " | Ambiente: "
                + environment
        );

        correo.setBody(
                "<html>" +
                        "<body>" +
                        "<h3>Nuevo proveedor recibido desde Graphite</h3>" +

                        "<p><b>Proveedor:</b> " + dto.getEntityPublicId() + "</p>" +
                        "<p><b>Nombre:</b> " + dto.getEntityName() + "</p>" +
                        "<p><b>ERP QAD:</b> " + dto.getErpIdQad() + "</p>" +
                        "<p><b>ERPs:</b> " + erps + "</p>" +
                        "<p><b>Ambiente:</b> " + environment + "</p>" +

                        "<p>El proveedor fue descargado correctamente desde Graphite y registrado en el sistema.</p>" +

                        "</body>" +
                "</html>"
        );

        correoPendienteRepository.save(correo);
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