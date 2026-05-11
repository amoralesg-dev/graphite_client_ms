package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlFrenosServiceImpl implements XmlFrenosService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        FrenosXmlFactory factory = new FrenosXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> "1000".equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                String erpId = "1000";

                SuppliersRowEntity supplier =
                        suppliersRowRepository
                                .findByCreditorCodeAndBusinessUnitCode(
                                        dto.getEntityPublicId(),
                                        erpId
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "No existe supplier en BD para "
                                                + dto.getEntityPublicId() + " / " + erpId
                                        )
                                );

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
            });
    }
}