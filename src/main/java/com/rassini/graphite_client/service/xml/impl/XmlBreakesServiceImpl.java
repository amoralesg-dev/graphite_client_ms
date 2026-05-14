package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.FrenosXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlBreakesServiceImpl implements XmlBreakesService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        // ✅ Breakes usa exactamente el MISMO factory que Frenos
        FrenosXmlFactory factory = new FrenosXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> "1850".equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                final String erpId = "1850";

                SuppliersRowEntity supplier =
                        suppliersRowRepository
                                .findBySupplierCodeAndBusinessUnitCode(
                                        dto.getEntityPublicId(),
                                        erpId
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "No existe supplier en BD para BREAKES "
                                                + dto.getEntityPublicId() + " / " + erpId
                                        )
                                );

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
            });
    }
}