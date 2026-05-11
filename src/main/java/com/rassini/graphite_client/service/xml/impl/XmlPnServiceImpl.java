package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlPnService;
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
public class XmlPnServiceImpl implements XmlPnService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;

    private String txzTaxZone = null;
    private String txclTaxCls =null;

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

                ContextInfoXml contextInfo= ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())
                .tiPriority("0")
                .ttRequestStartDate("NULL")
                .tiRequestStartTime("0")
                .tcCBFVersion("9,2")
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .build();
        
        BusinessRelationXml businessRelation = BusinessRelationXml.builder()
                .businessRelationCode(businessRelationCode)
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
                .tcCorporateGroupCode("PROVEEDOR")
                .tcLngCode("ls")
                .lastModifiedDate("")
                .lastModifiedTime("46780")
                .lastModifiedUser("mfg")
                .tc_Rowid("0x000000000005dfc3")
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
                .addressFormat("0")
                .addressIsTemporary("false")
                .txzTaxZone(this.txzTaxZone)
                .txclTaxCls(this.txclTaxCls)
                .addressIsSendToPostal("false")
                .addressIsTaxable("false")
                .addressIsTaxInCity("false")
                .addressIsTaxIncluded("false")
                .addressTaxIDFederal(supplier.getCreditorTaxIDFederal())
                .addressTaxIDState(supplier.getCreditorTaxIDFederal())
                .addressTaxDeclaration("0")
                .addressLogicKeyString("413826")
                .tcStateCode(supplier.getStateCode())
                .tcCountryCode(supplier.getCountryCode())
                .tcAddressTypeCode("HEADOFFICE")
                .tcLngCode("ls")
                .tcStateDescription("")
                .tcCountryDescription(supplier.getCountryCode())
                .tiCountryFormat("0")
                .tcLngDescription("latin spanish")
                .lastModifiedDate("")
                .lastModifiedTime("46780")
                .lastModifiedUser("mfg")
                .tc_Rowid("0x000000000005d382")
                .tc_ParentRowid("0x000000000005dfc3")
                .build();

        ContactXml contact = ContactXml.builder()
                .contactFunction("")
                .contactName(supplier.getContactName())
                .contactGender("MALE")
                .contactEmail(supplier.getContactEmail())
                .contactIsPrimary("true")
                .contactIsSecondary("false")
                .tcLngCode("ls")
                .lastModifiedDate("")
                .lastModifiedTime("46780")
                .lastModifiedUser("mfg")
                .tc_Rowid("0x000000000005e6c1")
                .tc_ParentRowid("0x000000000005d382")
                .build();

                XmlContext busrelCtx = XmlContext.builder()

                        // Output
                        .outputFileName(
                                "RPIEDRAS_busrel_" + supplier.getErpIDQAD() + ".xml"
                        )

                        .businessRelation(businessRelation)
                        .address(address)
                        .contact(contact)
                        .contextInfo(contextInfo)

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
                         .creditorTaxDeclaration("0")
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
                         .customInteger0("0")
                         .customInteger1("0")
                         .customInteger2("0")
                         .customInteger3("0")
                         .customInteger4("0")
                         .customDecimal0("0")
                         .customDecimal1("0")
                         .customDecimal2("0")
                         .customDecimal3("0")
                         .customDecimal4("0")
                         .customDecimal5("0")
                         .customDecimal6("0")
                         .customDecimal7("0")
                         .customDecimal8("0")
                         .customDecimal9("0")
                         .customDate5("NULL")
                         .customDate6("NULL")
                         .customDate7("NULL")
                         .customDate8("NULL")
                         .customDate9("NULL")
                         .lastModifiedDate("2026-4-13")
                         .lastModifiedTime("46783")
                         .lastModifiedUser("mfg")
                         .qADT01("NULL")
                         .qADD01("0")
                         .tc_Rowid("0x0000000000064615")
                         .build();     


                CreditorXmlContext creditorCtx =
                        CreditorXmlContext.builder()

                                .outputFileName(
                                        "RPIEDRAS_creditor_" + supplier.getErpIDQAD() + ".xml"
                                )
                                .contextInfo(contextInfo)
                                .creditor(creditor)
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