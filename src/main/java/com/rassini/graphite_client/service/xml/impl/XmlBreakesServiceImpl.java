package com.rassini.graphite_client.service.xml.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlContext;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
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

        // BREAKES = ERP 1850 (MISMO QUE FRENOS HOY)
        dto.getErpRecords().stream()
            .filter(erp -> "1850".equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                String erpId = "1850";

                SuppliersRowEntity supplier =
                        suppliersRowRepository
                                .findByCreditorCodeAndBusinessUnitCode(
                                        dto.getEntityPublicId(),   //
                                        erpId
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "No existe supplier en BD para BREAKES "
                                                + dto.getEntityPublicId() + " / " + erpId
                                        )
                                );

                // =========================
                // 1) BUSREL (BREAKES)
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

                // TAX (idéntico a Frenos POR AHORA)
                String txzTaxZone = null;
                List<String> taxZoneList = erp.getRassiniErpTaxZone();

                if (taxZoneList != null && !taxZoneList.isEmpty()
                        && taxZoneList.get(0) != null && !taxZoneList.get(0).isBlank()) {
                    txzTaxZone = taxZoneList.get(0);
                } else {
                    log.warn("[BREAKES][BUSREL] TxzTaxZone vacío supplier={}, ERP={}",
                            supplier.getErpIDQAD(), erpId);
                }

                String taxClassFromErp = erp.getRassiniErpTaxClass();
                String txclTaxCls =
                        (taxClassFromErp != null && !taxClassFromErp.isBlank())
                                ? taxClassFromErp
                                : catalogService.resolveTaxClass(erpId, taxClassFromErp);

                XmlContext busrelCtx = XmlContext.builder()

                        .outputFileName(
                                "busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                        )

                        .tcCompanyCode(erpId)
                        .lastModifiedDate("2026-4-13")
                        .lastModifiedTime("46780")
                        .lastModifiedUser("mfg")

                        .businessRelationCode(supplier.getErpIDQAD())
                        .entityName20(entityName20)
                        .tcCorporateGroupCode("PROVEEDOR")

                        .addressStreet1(supplier.getAddressStreet1())
                        .addressStreet2(supplier.getAddressStreet2())
                        .addressStreet3(supplier.getAddressStreet3())
                        .addressZip(supplier.getAddressZip())
                        .addressCity(supplier.getCityCode())
                        .addressName(supplier.getAddressStreet1())
                        .addressSearchName(entityName20)
                        .addressEmail(supplier.getContactEmail())

                        .txzTaxZone(txzTaxZone)
                        .txclTaxCls(txclTaxCls)
                        .rfc(supplier.getCreditorTaxIDFederal())
                        .rfcState(supplier.getCreditorTaxIDFederal())

                        .tcStateCode(supplier.getStateCode())
                        .tcCountryCode(supplier.getCountryCode())
                        .tcStateDescription("")
                        .tcCountryDescription("MEXICO")

                        .contactName(supplier.getContactName())
                        .contactEmail(supplier.getContactEmail())

                        .build();

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
                // 2) CREDITOR (BREAKES)
                // =========================
                String invProfile = "P_2010";
                String cnProfile = "P_2010";
                String prepayProfile = "P_2010";
                String divisionProfile = "0000";

                String paymentTermsFromErp = erp.getRassiniErpPaymentTerms();
                String paymentTerm =
                        (paymentTermsFromErp != null && !paymentTermsFromErp.isBlank())
                                ? paymentTermsFromErp
                                : "30";

                CreditorXmlContext creditorCtx =
                        CreditorXmlContext.builder()

                                .outputFileName(
                                        "creditor_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                                )

                                .tcCompanyCode(erpId)
                                .lastModifiedDate("2026-4-13")
                                .lastModifiedTime("46783")
                                .lastModifiedUser("mfg")

                                .creditorCode(supplier.getErpIDQAD())
                                .tcCurrencyCode(supplier.getCurrency())
                                .tcNormalPaymentConditionCode(paymentTerm)

                                .tcInvControlGLProfileCode(invProfile)
                                .tcCnControlGLProfileCode(cnProfile)
                                .tcPrepayControlGLProfileCode(prepayProfile)
                                .tcDivisionProfileCode(divisionProfile)

                                .build();

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