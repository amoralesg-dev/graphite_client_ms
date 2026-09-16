package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.BreakesXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import com.rassini.graphite_client.entity.XmlStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlBreakesServiceImpl implements XmlBreakesService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        //  Breakes usa exactamente el MISMO factory que Frenos pero ya tiene su propio archivo
        BreakesXmlFactory factory = new BreakesXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> XMLConstants.BREAKES.equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                final String erpId = XMLConstants.BREAKES;
                log.info("[XML-PROCESS] supplier={} businessUnit={} generator=BREAKES eligible=true", dto.getEntityPublicId(), erpId);

                Optional<SuppliersRowEntity> supplierOpt =
                        suppliersRowRepository
                                .findBySupplierCodeAndBusinessUnitCode(
                                        dto.getEntityPublicId(),
                                        erpId
                                );

                if (supplierOpt.isEmpty()) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=BREAKES result=ERROR reason=NO_ROW_IN_DB",
                            dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPBREAKES);
                    }
                    return;
                }

                SuppliersRowEntity supplier = supplierOpt.get();

                if (XmlStatus.ERROR.equals(supplier.getXmlStatus())) {
                    log.warn("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=ERROR", dto.getEntityPublicId(), erpId);
                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=SKIPPED reason=CATALOG_MAPPING_MISSING", dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPBREAKES);
                    }
                    return;
                }

                log.info("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=OK", dto.getEntityPublicId(), erpId);

                try {
                    // =========================
                    // BUSREL (BREAKES)
                    // =========================
                    XmlContext busrelCtx =
                            factory.buildBusrelContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone()
                            );

                    xmlGenerationHelper.generateIfFileNotExists(
                            supplier,
                            XmlConstants.OUTPUT_BREAKES_DIR,
                            busrelCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateBusinessRelationXml(
                                    XmlConstants.TEMPLATE_BREAKES_BUSREL,
                                    XmlConstants.OUTPUT_BREAKES_DIR,
                                    busrelCtx
                            )
                    );

                    // =========================
                    // CREDITOR (BREAKES)
                    // =========================
                    CreditorXmlContext creditorCtx =
                            factory.buildCreditorContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone(),
                                    erp.getRassiniErpPaymentTerms()
                            );

                    xmlGenerationHelper.generateIfFileNotExists(
                            supplier,
                            XmlConstants.OUTPUT_BREAKES_DIR,
                            creditorCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateCreditorXml(
                                    XmlConstants.TEMPLATE_BREAKES_CREDITOR,
                                    XmlConstants.OUTPUT_BREAKES_DIR,
                                    creditorCtx
                            )
                    );

                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=GENERATED files=[{}, {}]",
                            dto.getEntityPublicId(), erpId, busrelCtx.getOutputFileName(), creditorCtx.getOutputFileName());

                } catch (Exception e) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=BREAKES result=ERROR: {}",
                            dto.getEntityPublicId(), erpId, e.getMessage(), e);
                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPBREAKES);
                    }
                }
            });
    }
}