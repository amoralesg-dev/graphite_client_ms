package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlContext;
import com.rassini.graphite_client.service.xml.XmlPnService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlPnServiceImpl implements XmlPnService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    @Override
    public void generate(GraphiteSupplierDto dto) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        // =====================
        // PN = 09
        // =====================
        dto.getErpRecords().stream()
            .filter(erp -> "09".equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                String erpId = "09";

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
                // 1) BUSREL PN
                // =========================
                String entityName20 =
                        supplier.getBusinessRelationName1() == null
                                ? ""
                                : supplier.getBusinessRelationName1().substring(
                                        0,
                                        Math.min(
                                                supplier.getBusinessRelationName1().length(),
                                                20
                                        )
                                );

                // PN usa prefijo PR
                String businessRelationCode =
                        "PR" + supplier.getErpIDQAD();

                // --- TAX PN ---
                // TxzTaxZone: fijo MX
                String txzTaxZone = "MX";

                // TxclTaxCls: prioridad Graphite
                String taxClassFromErp = erp.getRassiniErpTaxClass();
                String txclTaxCls =
                        (taxClassFromErp != null && !taxClassFromErp.isBlank())
                                ? taxClassFromErp
                                : catalogService.resolveTaxClass(erpId, taxClassFromErp);

                XmlContext busrelCtx = XmlContext.builder()

                        // Output
                        .outputFileName(
                                "RPIEDRAS_busrel_" + supplier.getErpIDQAD() + ".xml"
                        )

                        // ContextInfo
                        .tcCompanyCode(erpId)
                        .lastModifiedDate("2026-4-13")
                        .lastModifiedTime("46780")
                        .lastModifiedUser("mfg")

                        // BusinessRelation
                        .businessRelationCode(businessRelationCode)
                        .entityName20(entityName20)
                        .tcCorporateGroupCode("PROVEEDOR")

                        // Address
                        .addressStreet1(supplier.getAddressStreet1())
                        .addressStreet2(supplier.getAddressStreet2())
                        .addressStreet3(supplier.getAddressStreet3())
                        .addressZip(supplier.getAddressZip())
                        .addressCity(supplier.getCityCode())
                        .addressName(supplier.getAddressStreet1())
                        .addressSearchName(entityName20)
                        .addressEmail(supplier.getContactEmail())

                        // Tax (PN)
                        .txzTaxZone(txzTaxZone)
                        .txclTaxCls(txclTaxCls)
                        .rfc(supplier.getCreditorTaxIDFederal())
                        .rfcState(supplier.getCreditorTaxIDFederal())

                        // Country / State
                        .tcStateCode(supplier.getStateCode())
                        .tcCountryCode(supplier.getCountryCode())
                        .tcStateDescription("")
                        .tcCountryDescription("MEXICO")

                        // Contact
                        .contactName(supplier.getContactName())
                        .contactEmail(supplier.getContactEmail())

                        .build();

                xmlGenerationHelper.generateIfFileNotExists(
                        supplier,
                        XmlConstants.OUTPUT_PN_DIR,
                        busrelCtx.getOutputFileName(),
                        log,
                        () -> xmlTemplateEngine.generateBusinessRelationXml(
                                XmlConstants.TEMPLATE_PN_BUSREL,
                                XmlConstants.OUTPUT_PN_DIR,
                                busrelCtx
                        )
                );

                // =========================
                // 2) CREDITOR PN
                // =========================
                String invProfile = "P_20010001";
                String cnProfile = "P_20010001";
                String prepayProfile = "P_20010001";
                String divisionProfile = "P_5001";
                String paymentTerm = "PN-04";

                CreditorXmlContext creditorCtx =
                        CreditorXmlContext.builder()

                                .outputFileName(
                                        "RPIEDRAS_creditor_" + supplier.getErpIDQAD() + ".xml"
                                )

                                // ContextInfo
                                .tcCompanyCode(erpId)
                                .lastModifiedDate("2026-4-13")
                                .lastModifiedTime("46783")
                                .lastModifiedUser("mfg")

                                // Creditor (PN usa PR)
                                .creditorCode(supplier.getErpIDQAD())
                                .tcCurrencyCode(supplier.getCurrency())
                                .tcNormalPaymentConditionCode(paymentTerm)

                                // GL Profiles
                                .tcInvControlGLProfileCode(invProfile)
                                .tcCnControlGLProfileCode(cnProfile)
                                .tcPrepayControlGLProfileCode(prepayProfile)
                                .tcDivisionProfileCode(divisionProfile)

                                .build();

                xmlGenerationHelper.generateIfFileNotExists(
                        supplier,
                        XmlConstants.OUTPUT_PN_DIR,
                        creditorCtx.getOutputFileName(),
                        log,
                        () -> xmlTemplateEngine.generateCreditorXml(
                                XmlConstants.TEMPLATE_PN_CREDITOR,
                                XmlConstants.OUTPUT_PN_DIR,
                                creditorCtx
                        )
                );
            });
    }
}