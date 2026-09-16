package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.OcXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import com.rassini.graphite_client.entity.XmlStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlOcServiceImpl implements XmlOcService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    /**
     * Orquestador por planta OC (0111 / 0301)
     */
    @Override
    public void generate(GraphiteSupplierDto dto , SupplierEntity supplierParameter) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        OcXmlFactory factory = new OcXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> {
                String id = erp.getRassiniErpEntityId();
                return XMLConstants.OC.equals(id) || XMLConstants.BYPASA.equals(id);
            })
            .forEach(erp -> {

                String erpId = erp.getRassiniErpEntityId();
                log.info("[XML-PROCESS] supplier={} businessUnit={} generator=OC eligible=true", dto.getEntityPublicId(), erpId);

                Optional<SuppliersRowEntity> supplierOpt = suppliersRowRepository
                        .findBySupplierCodeAndBusinessUnitCode(
                                dto.getEntityPublicId(),
                                erpId
                        );

                if (supplierOpt.isEmpty()) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=OC result=ERROR reason=NO_ROW_IN_DB",
                            dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                    return;
                }

                SuppliersRowEntity supplier = supplierOpt.get();

                if (XmlStatus.ERROR.equals(supplier.getXmlStatus())) {
                    log.warn("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=ERROR", dto.getEntityPublicId(), erpId);
                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=SKIPPED reason=CATALOG_MAPPING_MISSING", dto.getEntityPublicId(), erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                    return;
                }

                log.info("[XML-PROCESS] supplier={} businessUnit={} catalogStatus=OK", dto.getEntityPublicId(), erpId);

                try {
                    log.debug(
                    "[TAX-DEBUG] erpId={} taxClass='{}' taxZone={}",
                    erpId,
                    erp.getRassiniErpTaxClass(),
                    erp.getRassiniErpTaxZone()
                    );
                    log.debug(
                    "[TERMS-DEBUG] erpId={} ErpPaymentTerms='{}'",
                    erpId,
                    erp.getRassiniErpPaymentTerms()
                    );

                    // =====================================================
                    // BUSREL
                    // =====================================================
                    XmlContext busrelCtx =
                            factory.buildBusrelContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone()
                            );

                    xmlGenerationHelper.generateIfFileNotExists(
                            supplier,
                            XmlConstants.OUTPUT_OC_DIR,
                            busrelCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateBusinessRelationXml(
                                    XmlConstants.TEMPLATE_OC_BUSREL,
                                    XmlConstants.OUTPUT_OC_DIR,
                                    busrelCtx
                            )
                    );

                    // =====================================================
                    // CREDITOR
                    // =====================================================
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
                            XmlConstants.OUTPUT_OC_DIR,
                            creditorCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateCreditorXml(
                                    XmlConstants.TEMPLATE_OC_CREDITOR,
                                    XmlConstants.OUTPUT_OC_DIR,
                                    creditorCtx
                            )
                    );

                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=GENERATED files=[{}, {}]",
                            dto.getEntityPublicId(), erpId, busrelCtx.getOutputFileName(), creditorCtx.getOutputFileName());

                } catch (Exception e) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=OC result=ERROR: {}",
                            dto.getEntityPublicId(), erpId, e.getMessage(), e);
                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                }
            });
    }


    @Override
    public void generateBusinessRelation(GraphiteSupplierDto dto) {
        // Se genera por planta dentro de generate()
    }

    @Override
    public void generateCreditor(GraphiteSupplierDto dto) {
        // Se genera por planta dentro de generate()
    }
}