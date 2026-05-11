package com.rassini.graphite_client.service.xml.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.AddressXml;
import com.rassini.graphite_client.service.xml.context.BusinessRelationXml;
import com.rassini.graphite_client.service.xml.context.ContactXml;
import com.rassini.graphite_client.service.xml.context.ContextInfoXml;
import com.rassini.graphite_client.service.xml.context.CreditorNodoXML;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
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

    private String txzTaxZone = null;
    private String txclTaxCls = null;

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
                    log.warn("[FRENOS][BUSREL] TxzTaxZone NO informado en Graphite para supplier={}, erpId={}, ERP QAD={}",
                            supplier.getCreditorCode(), erpId,supplier.getErpIDQAD());
                }

                String taxClassFromErp = erp.getRassiniErpTaxClass();
                String txclTaxCls = (taxClassFromErp != null && !taxClassFromErp.isBlank())
                        ? taxClassFromErp
                        : catalogService.resolveTaxClass(erpId, taxClassFromErp); // con tu CatalogService corregido, esto tenderá a null si no hay valor

                if (txclTaxCls == null || txclTaxCls.isBlank()) {
                    log.warn("[FRENOS][BUSREL] TxclTaxCls NO resuelto (Graphite vacío y sin catálogo) para supplier={}, erpId={}, ERP QAD={}",
                            supplier.getCreditorCode(), erpId, supplier.getErpIDQAD());
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

                XmlContext busrelCtx = XmlContext.builder()

                        // Output
                        .outputFileName(
                                "busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml"
                        )
                        .address(address)
                        .contact(contact)
                        .businessRelation(businessRelation)
                        .contextInfo(contextInfo)
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
                String paymentTermsFromErp = erp.getRassiniErpPaymentTerms(); //
                String paymentTerm = (paymentTermsFromErp != null && !paymentTermsFromErp.isBlank())
                        ? paymentTermsFromErp
                        : "30";

                if (paymentTermsFromErp == null || paymentTermsFromErp.isBlank()) {
                    log.warn("[FRENOS][CREDITOR] PaymentTerms NO informado en Graphite, usando fallback='30' para supplier={}, erpId={}, ERP QAD={}",
                            supplier.getCreditorCode(), erpId, supplier.getErpIDQAD());
                }

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
                         .tcNormalPaymentConditionCode(paymentTerm)
                         .tcInvControlGLProfileCode(invProfile)
                         .tcCnControlGLProfileCode(cnProfile)
                         .tcPrepayControlGLProfileCode(prepayProfile)
                         .tcDivisionProfileCode(divisionProfile)
                         .tcReasonCode("INV TO APPROVE")
                         .tlBusinessRelationIsInterco("false")
                         .tcBusinessRelationCode(supplier.getErpIDQAD())
                         .tcCurrencyCode(supplier.getCurrency())
                         .tcCreditorTypeCode("NC")
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



                CreditorXmlContext creditorCtx =
                        CreditorXmlContext.builder()

                                .outputFileName(
                                        "creditor_" + supplier.getCreditorCode() + "_" + erpId + ".xml"
                                )

                                .contextInfo(contextInfo)
                                .creditor(creditor)
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