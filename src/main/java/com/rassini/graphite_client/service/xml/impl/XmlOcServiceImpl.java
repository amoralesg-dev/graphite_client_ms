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
        if (supplierParameter != null
            && ProviderState.ERRORMAPPING.equals(supplierParameter.getStatus())) {
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

                SuppliersRowEntity supplier = suppliersRowRepository
                        .findBySupplierCodeAndBusinessUnitCode(
                                dto.getEntityPublicId(),
                                erpId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe supplier en BD para "
                                                + dto.getEntityPublicId()
                                                + " / " + erpId
                                )
                        );
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