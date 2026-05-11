package com.rassini.graphite_client.service.xml.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlBreakesService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.context.AddressXml;
import com.rassini.graphite_client.service.xml.context.BusinessRelationXml;
import com.rassini.graphite_client.service.xml.context.ContactXml;
import com.rassini.graphite_client.service.xml.context.ContextInfoXml;
import com.rassini.graphite_client.service.xml.context.CreditorNodoXML;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
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

        // BREAKES = 1850
        dto.getErpRecords().stream()
            .filter(erp -> "1850".equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {

                final String erpId = "1850";

                // ✅ MISMO lookup que Frenos (sin cambios)
                final String creditorKey = dto.getEntityPublicId();

                SuppliersRowEntity supplier =
                        suppliersRowRepository
                                .findByCreditorCodeAndBusinessUnitCode(
                                        creditorKey,
                                        erpId
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "No existe supplier en BD para BREAKES "
                                                + creditorKey + " / " + erpId
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

                // ----- TAX BREAKES (1850) = IGUAL A FRENOS POR AHORA -----
                String txzTaxZone = null;
                List<String> taxZoneList = erp.getRassiniErpTaxZone();
                if (taxZoneList != null && !taxZoneList.isEmpty()
                        && taxZoneList.get(0) != null && !taxZoneList.get(0).isBlank()) {
                    txzTaxZone = taxZoneList.get(0);
                } else {
                    log.warn("[BREAKES][BUSREL] TxzTaxZone NO informado supplier={}, erpId={}, ERP QAD={}",
                            supplier.getCreditorCode(), erpId, supplier.getErpIDQAD());
                }

                String taxClassFromErp = erp.getRassiniErpTaxClass();
                String txclTaxCls =
                        (taxClassFromErp != null && !taxClassFromErp.isBlank())
                                ? taxClassFromErp
                                : catalogService.resolveTaxClass(erpId, taxClassFromErp);

                if (txclTaxCls == null || txclTaxCls.isBlank()) {
                    log.warn("[BREAKES][BUSREL] TxclTaxCls NO resuelto supplier={}, erpId={}, ERP QAD={}",
                            supplier.getCreditorCode(), erpId, supplier.getErpIDQAD());
                }

                ContextInfoXml contextInfo = ContextInfoXml.builder()
                        .tcCompanyCode(erpId)
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
                        .outputFileName("busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml")
                        .contextInfo(contextInfo)
                        .businessRelation(businessRelation)
                        .address(address)
                        .contact(contact)
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
                        .txzTaxZone(txzTaxZone)
                        .txclTaxCls(txclTaxCls)
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
                        .lastModifiedDate("2026-4-13")
                        .lastModifiedTime("46783")
                        .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                        .qADT01("NULL")
                        .qADD01(XMLConstants.CERO)
                        .tc_Rowid("0x0000000000064615")
                        .build();

                CreditorXmlContext creditorCtx = CreditorXmlContext.builder()
                        .outputFileName("creditor_" + supplier.getCreditorCode() + "_" + erpId + ".xml")
                        .contextInfo(contextInfo)
                        .creditor(creditor)
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