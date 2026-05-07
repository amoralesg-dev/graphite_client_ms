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
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;

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
     * Orquestador por planta (OC = 0111, 0301)
     */
    @Override
    public void generate(GraphiteSupplierDto dto) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        dto.getErpRecords().stream()
            .filter(erp -> {
                String id = erp.getRassiniErpEntityId();
                return "0111".equals(id) || "0301".equals(id);
            })
            .forEach(erp -> {

                String erpId = erp.getRassiniErpEntityId();

                SuppliersRowEntity supplier = suppliersRowRepository
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

                // BusinessRelation XML (busrel)
                generateBusinessRelationInternal(
                      supplier,
                      erpId,
                      erp.getRassiniErpTaxClass(),
                      erp.getRassiniErpTaxZone()
              );

                // Creditor XML (creditor)
                generateCreditorInternal(
                        supplier,
                        erpId
                );
            });
    }

    // =========================================================
    // ========== IMPLEMENTACIÓN DE INTERFACES =================
    // =========================================================

    @Override
    public void generateBusinessRelation(GraphiteSupplierDto dto) {
        // No se usa directo: la generación real va por planta en generate(...)
    }

    @Override
    public void generateCreditor(GraphiteSupplierDto dto) {
        // No se usa directo: la generación real va por planta en generate(...)
    }

    // =========================================================
    // ========== IMPLEMENTACIÓN REAL ==========================
    // =========================================================

    /**
     * BUSREL = ContextInfo + BusinessRelation + Address + Contact
     * Usa XmlContext (tu clase real actual).
     */
    
private void generateBusinessRelationInternal(
        SuppliersRowEntity supplier,
        String erpId,
        String taxClassFromErp,
        List<String> taxZoneFromErp
    ) {

        String entityName20 =
                supplier.getBusinessRelationName1() == null
                        ? ""
                        : supplier.getBusinessRelationName1().substring(
                                0,
                                Math.min(supplier.getBusinessRelationName1().length(), 20)
                        );

        // =====================================================
        // TAX OC (0111 / 0301)
        // =====================================================
        String txzTaxZone = null;
        String txclTaxCls = null;

        if ("0111".equals(erpId) || "0301".equals(erpId)) {

            // ---- TxzTaxZone ----
            // PRIORIDAD: Graphite
            if (taxZoneFromErp != null
                    && !taxZoneFromErp.isEmpty()
                    && taxZoneFromErp.get(0) != null
                    && !taxZoneFromErp.get(0).isBlank()) {

                txzTaxZone = taxZoneFromErp.get(0);

            } else {
                txzTaxZone = "MX";
            }

            // ---- TxclTaxCls ----
            // PRIORIDAD: Graphite
            if (taxClassFromErp != null && !taxClassFromErp.isBlank()) {

                txclTaxCls = taxClassFromErp;

            } else {
                // Fallback controlado 
                txclTaxCls = catalogService.resolveTaxClass(
                        erpId,
                        taxClassFromErp
                );
            }
        }

        XmlContext ctx = XmlContext.builder()

                // ===== Output =====
                .outputFileName(
                        "busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                )

                // ===== ContextInfo =====
                .tcCompanyCode(erpId)
                .lastModifiedDate("2026-4-13")
                .lastModifiedTime("46780")
                .lastModifiedUser("mfg")

                // ===== BusinessRelation =====
                .businessRelationCode(supplier.getErpIDQAD())
                .entityName20(entityName20)
                .tcCorporateGroupCode("PROVEEDOR")

                // ===== Address =====
                .addressStreet1(supplier.getAddressStreet1())
                .addressStreet2(supplier.getAddressStreet2())
                .addressStreet3(supplier.getAddressStreet3())
                .addressZip(supplier.getAddressZip())
                .addressCity(supplier.getCityCode())
                .addressName(supplier.getAddressStreet1())
                .addressSearchName(entityName20)
                .addressEmail(supplier.getContactEmail())

                // ===== Tax =====
                .txzTaxZone(txzTaxZone)
                .txclTaxCls(txclTaxCls)
                .rfc(supplier.getCreditorTaxIDFederal())
                .rfcState(supplier.getCreditorTaxIDFederal())

                // ===== Country / State =====
                .tcStateCode(supplier.getStateCode())
                .tcCountryCode(supplier.getCountryCode())
                .tcStateDescription("")          // QAD lo completa
                .tcCountryDescription("MEXICO")

                // ===== Contact =====
                .contactName(supplier.getContactName())
                .contactEmail(supplier.getContactEmail())

                .build();

        xmlGenerationHelper.generateIfFileNotExists(
                supplier,
                XmlConstants.OUTPUT_OC_DIR,
                ctx.getOutputFileName(),
                log,
                () -> xmlTemplateEngine.generateBusinessRelationXml(
                        XmlConstants.TEMPLATE_OC_BUSREL,
                        XmlConstants.OUTPUT_OC_DIR,
                        ctx
                )
        );
    }
    /**
     * CREDITOR = ContextInfo + Creditor
     * Usa CreditorXmlContext (NO XmlContext).
     *
     * Nota: aquí NO usamos supplier.getCurrencyCode() ni supplier.getPaymentTermCode()
     * porque en tu entity no existen. Se dejan valores configurables por planta.
     */
    private void generateCreditorInternal(SuppliersRowEntity supplier,
                                          String erpId) {

        // Valores base por planta (si ya tienes el archivo de César, aquí se reemplaza por resolver)
        // Basado en tus XML reales:
        // 0111: P_2010 / Sub_0620
        // 0301: si es igual, queda igual; si no, ajustas aquí.
        String invProfile = "P_2010";
        String cnProfile = "P_2010";
        String prepayProfile = "P_2010";
        String divisionProfile = "Sub_0620";

        // Payment term OC real típico: "30"
        String paymentTerm = "30";

        // Currency: en tu entity existe "currency" (getter = getCurrency())
        // Si viene null, el engine pondrá vacío (no rompe).
        String currency = supplier.getCurrency();

        CreditorXmlContext ctx =
                CreditorXmlContext.builder()

                        // ===== Output =====
                        .outputFileName(
                                "creditor_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                        )

                        // ===== ContextInfo =====
                        .tcCompanyCode(erpId)
                        .lastModifiedDate("2026-4-13")
                        .lastModifiedTime("46783")
                        .lastModifiedUser("mfg")

                        // ===== Creditor =====
                        .creditorCode(supplier.getErpIDQAD())
                        .tcCurrencyCode(currency)
                        .tcNormalPaymentConditionCode(paymentTerm)

                        // ===== GL Profiles =====
                        .tcInvControlGLProfileCode(invProfile)
                        .tcCnControlGLProfileCode(cnProfile)
                        .tcPrepayControlGLProfileCode(prepayProfile)
                        .tcDivisionProfileCode(divisionProfile)

                        .build();
        xmlGenerationHelper.generateIfFileNotExists(
                supplier,
                XmlConstants.OUTPUT_OC_DIR,
                ctx.getOutputFileName(),
                log,
                () -> xmlTemplateEngine.generateCreditorXml(
                        XmlConstants.TEMPLATE_OC_CREDITOR,
                        XmlConstants.OUTPUT_OC_DIR,
                        ctx
                )
        );

    }
}