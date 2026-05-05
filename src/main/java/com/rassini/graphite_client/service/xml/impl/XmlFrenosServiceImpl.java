package com.rassini.graphite_client.service.xml.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlContext;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
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

        // Frenos = 1000
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
                // 1) BUSREL (FRENOS)
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

                // -------------------------
                // TAX (FRENOS / 1000)
                // TxzTaxZone: viene de Graphite RASSINI_ERP_Tax_Zone[0] (NO default)
                // TxclTaxCls: viene de Graphite RASSINI_ERP_Tax_Class; si no viene, queda null (hasta tener dataLstTaxClass)
                // -------------------------
                String txzTaxZone = null;
                List<String> taxZoneList = erp.getRassiniErpTaxZone();
                if (taxZoneList != null && !taxZoneList.isEmpty()
                        && taxZoneList.get(0) != null && !taxZoneList.get(0).isBlank()) {
                    txzTaxZone = taxZoneList.get(0);
                } else {
                    log.warn("[FRENOS][BUSREL] TxzTaxZone NO informado en Graphite para supplier={}, erpId={}",
                            supplier.getCreditorCode(), erpId);
                }

                String taxClassFromErp = erp.getRassiniErpTaxClass();
                String txclTaxCls = (taxClassFromErp != null && !taxClassFromErp.isBlank())
                        ? taxClassFromErp
                        : catalogService.resolveTaxClass(erpId, taxClassFromErp); // con tu CatalogService corregido, esto tenderá a null si no hay valor

                if (txclTaxCls == null || txclTaxCls.isBlank()) {
                    log.warn("[FRENOS][BUSREL] TxclTaxCls NO resuelto (Graphite vacío y sin catálogo) para supplier={}, erpId={}",
                            supplier.getCreditorCode(), erpId);
                }

                XmlContext busrelCtx = XmlContext.builder()

                        // Output
                        .outputFileName(
                                "busrel_" + supplier.getCreditorCode() + "_" + erpId + ".xml"
                        )

                        // ContextInfo
                        .tcCompanyCode(erpId)
                        .lastModifiedDate("2026-4-13")
                        .lastModifiedTime("46780")
                        .lastModifiedUser("mfg")

                        // BusinessRelation
                        .businessRelationCode(supplier.getCreditorCode())
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

                        // Tax (FRENOS)
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
                // 2) CREDITOR (FRENOS)
                // =========================
                String invProfile = "P_2010";
                String cnProfile = "P_2010";
                String prepayProfile = "P_2010";
                String divisionProfile = "0000";   // típico Frenos

                // Payment terms (FRENOS / 1000):
                // regla: si Graphite trae RASSINI_ERP_Payment_Terms -> usarlo.
                // si NO trae, se mantiene "30" (histórico) pero se deja log de advertencia.
                String paymentTermsFromErp = erp.getRassiniErpPaymentTerms(); // <-- ajusta nombre si tu getter difiere
                String paymentTerm = (paymentTermsFromErp != null && !paymentTermsFromErp.isBlank())
                        ? paymentTermsFromErp
                        : "30";

                if (paymentTermsFromErp == null || paymentTermsFromErp.isBlank()) {
                    log.warn("[FRENOS][CREDITOR] PaymentTerms NO informado en Graphite, usando fallback='30' para supplier={}, erpId={}",
                            supplier.getCreditorCode(), erpId);
                }

                CreditorXmlContext creditorCtx =
                        CreditorXmlContext.builder()

                                .outputFileName(
                                        "creditor_" + supplier.getCreditorCode() + "_" + erpId + ".xml"
                                )

                                // ContextInfo
                                .tcCompanyCode(erpId)
                                .lastModifiedDate("2026-4-13")
                                .lastModifiedTime("46783")
                                .lastModifiedUser("mfg")

                                // Creditor
                                .creditorCode(supplier.getCreditorCode())
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