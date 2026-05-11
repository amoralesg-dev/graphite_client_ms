package com.rassini.graphite_client.service.xml.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.AddressXml;
import com.rassini.graphite_client.service.xml.context.BusinessRelationXml;
import com.rassini.graphite_client.service.xml.context.ContactXml;
import com.rassini.graphite_client.service.xml.context.ContextInfoXml;
import com.rassini.graphite_client.service.xml.context.CreditorNodoXML;
import com.rassini.graphite_client.service.xml.context.CreditorXML;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
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
    String txzTaxZone = null;
    String txclTaxCls = null;

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

        ContextInfoXml contextInfo= ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate("NULL")
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .build();
        
        BusinessRelationXml businessRelation = BusinessRelationXml.builder()
                .businessRelationCode(supplier.getErpIDQAD())
                .businessRelationName1(supplier.getBusinessRelationName1())
                .businessRelationName2(supplier.getBusinessRelationName1())
                .businessRelationName3(supplier.getBusinessRelationName1())
                .businessRelationSearchName(entityName20)
                .businessRelationIsActive("true")
                .businessRelationIsInterco("false")
                .businessRelationIsInComp("false")
                .businessRelationIsCompens("true")
                .businessRelationIsTaxRep("false")
                .businessRelationIsLastFill("false")
                .businessRelationIsDomRestr("false")
                .tcCorporateGroupCode(XMLConstants.PROVEEDOR)
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate("")
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.PARENT_ROW_ID)
                .build();

        AddressXml address = AddressXml.builder()
                .addressStreet1(supplier.getAddressStreet1())
                .addressStreet2(supplier.getAddressStreet2())
                .addressStreet3(supplier.getAddressStreet3())
                .addressZip(supplier.getAddressZip())
                .addressCity(supplier.getCityCode())
                .addressCityCode(supplier.getCityCode())
                .addressName(supplier.getAddressStreet1())
                .addressSearchName(entityName20)
                .addressTelephone("")
                .addressEMail("")
                .addressFormat(XMLConstants.CERO)
                .addressIsTemporary("false")
                .txzTaxZone(txzTaxZone)
                .txclTaxCls(txclTaxCls)
                .addressIsSendToPostal("false")
                .addressIsTaxable("false")
                .addressIsTaxInCity("false")
                .addressIsTaxIncluded("false")
                .addressTaxIDFederal(supplier.getCreditorTaxIDFederal())
                .addressTaxIDState(supplier.getCreditorTaxIDFederal())
                .addressTaxDeclaration(XMLConstants.CERO)
                .addressLogicKeyString(XMLConstants.ADDRESS_LOGIC_KEY)
                .tcStateCode(supplier.getStateCode())
                .tcCountryCode(supplier.getCountryCode())
                .tcAddressTypeCode("HEADOFFICE")
                .tcLngCode(XMLConstants.LANG_CODE)
                .tcStateDescription("")
                .tcCountryDescription(supplier.getCountryCode())
                .tiCountryFormat(XMLConstants.CERO)
                .tcLngDescription("latin spanish")
                .lastModifiedDate("")
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.ROW_ID)
                .tc_ParentRowid(XMLConstants.PARENT_ROW_ID)
                .build();

        ContactXml contact = ContactXml.builder()
                .contactFunction("")
                .contactName(supplier.getContactName())
                .contactGender(XMLConstants.CONTACT_MALE)
                .contactEmail(supplier.getContactEmail())
                .contactIsPrimary("true")
                .contactIsSecondary("false")
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate("")
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.CONTACT_ROW_ID)
                .tc_ParentRowid(XMLConstants.ROW_ID)
                .build();

        XmlContext ctx = XmlContext.builder()

                // ===== Output =====
                .outputFileName(
                        "busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                )
                .contextInfo(contextInfo)
                .businessRelation(businessRelation)
                .address(address)
                .contact(contact)
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

         ContextInfoXml contextInfo=ContextInfoXml.builder()
                // ===== ContextInfo  =====
                .tcCompanyCode(Integer.parseInt(erpId)+"")
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate("NULL")
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .build();

         CreditorNodoXML creditor = CreditorNodoXML.builder()
                         .creditorIsActive("false")
                         .creditorCode(supplier.getErpIDQAD())
                         .vatDeliveryType("PRODUCT")
                         .vatPercentageLevel("NONE")
                         .creditorIsSendRemittance("false")
                         .creditorIsIndividualPaymnt("false")
                         .creditorIsTaxable("false")
                         .creditorIsTaxInCity("false")
                         .creditorIsTaxIncluded("false")
                         .creditorTaxIDFederal(supplier.getCreditorTaxIDFederal())
                         .creditorTaxIDState(supplier.getCreditorTaxIDFederal())
                         .creditorTaxDeclaration(XMLConstants.CERO)
                         .creditorIsTaxReport("false")
                         .creditorIsTaxConfirmed("false")
                         .creditorIsWHT("false")
                         .creditorIsBearBankCharge("false")
                         .creditorBirthDate("NULL")
                         .txzTaxZone(this.txzTaxZone)
                         .txclTaxCls(this.txclTaxCls)
                         .tcNormalPaymentConditionCode("")
                         .tcInvControlGLProfileCode(invProfile)
                         .tcCnControlGLProfileCode(cnProfile)
                         .tcPrepayControlGLProfileCode(prepayProfile)
                         .tcDivisionProfileCode(divisionProfile)
                         .tcReasonCode("INV TO APPROVE")
                         .tlBusinessRelationIsInterco("false")
                         .tcBusinessRelationCode(supplier.getErpIDQAD())
                         .tcCurrencyCode(currency)
                         .tcCreditorTypeCode("")
                         .tcNormalPaymentConditionType("NORMAL")
                         .tcPurchaseGLProfileCode("P_Compras")
                         .tcBusinessRelationName1(supplier.getBusinessRelationName1())
                         .tcPurchaseTypeCode("OTRO")
                         .customDate0("NULL")
                         .customDate1("NULL")
                         .customDate2("NULL")
                         .customDate3("NULL")
                         .customDate4("NULL")
                         .customInteger0(XMLConstants.CERO)
                         .customInteger1(XMLConstants.CERO)
                         .customInteger2(XMLConstants.CERO)
                         .customInteger3(XMLConstants.CERO)
                         .customInteger4(XMLConstants.CERO)
                         .customDecimal0(XMLConstants.CERO)
                         .customDecimal1(XMLConstants.CERO)
                         .customDecimal2(XMLConstants.CERO)
                         .customDecimal3(XMLConstants.CERO)
                         .customDecimal4(XMLConstants.CERO)
                         .customDecimal5(XMLConstants.CERO)
                         .customDecimal6(XMLConstants.CERO)
                         .customDecimal7(XMLConstants.CERO)
                         .customDecimal8(XMLConstants.CERO)
                         .customDecimal9(XMLConstants.CERO)
                         .customDate5("NULL")
                         .customDate6("NULL")
                         .customDate7("NULL")
                         .customDate8("NULL")
                         .customDate9("NULL")
                         .lastModifiedDate("2026-4-13")
                         .lastModifiedTime("46783")
                         .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                         .qADT01("NULL")
                         .qADD01(XMLConstants.CERO)
                         .tc_Rowid("0x0000000000064615")
                         .build();     



        CreditorXmlContext ctx =
                CreditorXmlContext.builder()

                        // ===== Output =====
                        .outputFileName(
                                "creditor_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                        )
                        .contextInfo(contextInfo)
                        .creditor(creditor)
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