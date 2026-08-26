package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlPn99Service;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.Pn99XmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlPn99ServiceImpl implements XmlPn99Service {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }
        if (supplierParameter != null
            && ProviderState.ERRORMAPPING.equals(supplierParameter.getStatus())) {
                return;
        }

        Pn99XmlFactory factory = new Pn99XmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> XMLConstants.PN99.equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                try{

                        String erpId = XMLConstants.PN99;

                        SuppliersRowEntity supplier =
                                suppliersRowRepository
                                        .findBySupplierCodeAndBusinessUnitCode(
                                                dto.getEntityPublicId(),
                                                erpId
                                        )
                                        .orElseThrow(() ->
                                                new IllegalStateException(
                                                        "No existe supplier en BD para "
                                                        + dto.getEntityPublicId() + " / " + erpId
                                                )
                                        );
                                
                        String txzone=erp.getRassiniErpTaxZone() != null
                                && !erp.getRassiniErpTaxZone().isEmpty()
                                ? erp.getRassiniErpTaxZone().get(0)
                                : null;
                                        
                        // =========================
                        // BUSREL PN
                        // =========================
                        XmlContext busrelCtx =
                                factory.buildBusrelContext(
                                        supplier,
                                        erp.getRassiniErpTaxClass(),
                                        txzone
                                );

                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                XmlConstants.OUTPUT_PN99_DIR,
                                busrelCtx.getOutputFileName(),
                                log,
                                () -> xmlTemplateEngine.generateBusinessRelationXml(
                                        XmlConstants.TEMPLATE_PN99_BUSREL,
                                        XmlConstants.OUTPUT_PN99_DIR,
                                        busrelCtx
                                )
                        );

                        // =========================
                        // CREDITOR PN
                        // =========================

                        

                        CreditorXmlContext creditorCtx =
                                factory.buildCreditorContext(
                                        supplier,
                                        erp.getRassiniErpTaxClass(),
                                        txzone
                                );

                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                XmlConstants.OUTPUT_PN99_DIR,
                                creditorCtx.getOutputFileName(),
                                log,
                                () -> xmlTemplateEngine.generateCreditorXml(
                                        XmlConstants.TEMPLATE_PN99_CREDITOR,
                                        XmlConstants.OUTPUT_PN99_DIR,
                                        creditorCtx
                                )
                        );
                        
                } catch (IllegalStateException e) {

                        
                        log.error(
                                "Error generando XML PN para proveedor {}: {}",
                                dto.getEntityPublicId(),
                                e.getMessage(),
                                e
                        );

                        if (supplierParameter != null) {
                                supplierParameter.setStatus(ProviderState.ERRORMAPPN);
                        }

                        return;


                }

            });
            
        if (supplierParameter != null
                && !ProviderState.ERRORMAPPN.equals(supplierParameter.getStatus())) {
                supplierParameter.setStatus(ProviderState.PROCESSINGXMLPN);
        }

    }
}