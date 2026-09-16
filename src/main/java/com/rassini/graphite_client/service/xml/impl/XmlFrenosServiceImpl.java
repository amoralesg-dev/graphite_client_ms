package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.FrenosXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import com.rassini.graphite_client.entity.XmlStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlFrenosServiceImpl implements XmlFrenosService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        FrenosXmlFactory factory = new FrenosXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> XMLConstants.FRENOS.equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                String erpId = XMLConstants.FRENOS;
                log.info("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS eligible=true", dto.getEntityPublicId(), erpId);

                Optional<SuppliersRowEntity> supplierOpt =
                        suppliersRowRepository
                                .findBySupplierCodeAndBusinessUnitCode(
                                        dto.getEntityPublicId(),
                                        erpId
                                );

                if (supplierOpt.isEmpty()) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS result=ERROR reason=NO_ROW_IN_DB",
                            dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                    return;
                }

                SuppliersRowEntity supplier = supplierOpt.get();

                if (XmlStatus.ERROR.equals(supplier.getXmlStatus())) {
                    log.warn("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=ERROR", dto.getEntityPublicId(), erpId);
                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=SKIPPED reason=CATALOG_MAPPING_MISSING", dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                    return;
                }

                log.info("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=OK", dto.getEntityPublicId(), erpId);

                try {
                    // =========================
                    // BUSREL FRENOS
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
                            XmlConstants.OUTPUT_FRENOS_DIR,
                            busrelCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateBusinessRelationXml(
                                    XmlConstants.TEMPLATE_FRENOS_BUSREL,
                                    XmlConstants.OUTPUT_FRENOS_DIR,
                                    busrelCtx
                            )
                    );

                    // =========================
                    // CREDITOR FRENOS
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
                            XmlConstants.OUTPUT_FRENOS_DIR,
                            creditorCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateCreditorXml(
                                    XmlConstants.TEMPLATE_FRENOS_CREDITOR,
                                    XmlConstants.OUTPUT_FRENOS_DIR,
                                    creditorCtx
                            )
                    );

                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=GENERATED files=[{}, {}]",
                            dto.getEntityPublicId(), erpId, busrelCtx.getOutputFileName(), creditorCtx.getOutputFileName());

                } catch (Exception e) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS result=ERROR: {}",
                            dto.getEntityPublicId(), erpId, e.getMessage(), e);
                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                }
            });
    }
}