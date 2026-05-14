package com.rassini.graphite_client.service.xml.factory;

import java.util.List;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.context.*;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules.Domain;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

public class OcXmlFactory {

    private final CatalogService catalogService;

    public OcXmlFactory(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // =====================================================
    // BUSREL CONTEXT
    // =====================================================

    public XmlContext buildBusrelContext(
            SuppliersRowEntity supplier,
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp
    ) {

        TaxInfo tax = resolveTaxInfo(erpId, taxClassFromErp, taxZoneFromErp);
        String name20 = left(supplier.getSupplierName(), 20);

        return XmlContext.builder()
                .outputFileName("busrel_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier, name20))
                .address(buildAddress(supplier, name20, tax))
                .contact(buildContact(supplier))
                .build();
    }

    // =====================================================
    //  CREDITOR CONTEXT
    // =====================================================

    public CreditorXmlContext buildCreditorContext(
            SuppliersRowEntity supplier,
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp
    ) {
        TaxInfo tax = resolveTaxInfo(erpId, taxClassFromErp, taxZoneFromErp);

        return CreditorXmlContext.builder()
                .outputFileName("creditor_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoCreditor(erpId))
                .creditor(buildCreditor(supplier, tax))
                .build();
    }

    // =====================================================
    // CONTEXT INFO
    // =====================================================

    private ContextInfoXml buildContextInfoBusrel(SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())
                .tcAction("")
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate(XMLConstants.NULL)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcComment("")
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcComponentVersion("")
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .tcPartialUpdateExceptionList("")
                .build();
    }

    private ContextInfoXml buildContextInfoCreditor(String erpId) {
        return ContextInfoXml.builder()
                .tcCompanyCode(erpId)
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate(XMLConstants.NULL)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .build();
    }

    // =====================================================
    // BUSINESS RELATION
    // =====================================================

    private BusinessRelationXml buildBusinessRelation(
            SuppliersRowEntity supplier,
            String name20
    ) {
        return BusinessRelationXml.builder()
                .businessRelationCode(supplier.getErpIdQad())
                .businessRelationName1(supplier.getSupplierName())
                .businessRelationName2(supplier.getSupplierName())
                .businessRelationName3(supplier.getSupplierName())
                .businessRelationSearchName(name20)
                .businessRelationIsActive("true")
                .businessRelationIsInterco("false")
                .businessRelationIsInComp("false")
                .businessRelationIsCompens("true")
                .businessRelationIsTaxRep("false")
                .businessRelationIsLastFill("false")
                .businessRelationIsDomRestr("false")
                .tcCorporateGroupCode(XMLConstants.PROVEEDOR)
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate(XMLConstants.OC_LAST_MODIFIED_DATE)
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.PARENT_ROW_ID)
                .tc_ParentRowid("")
                .build();
    }

    // =====================================================
    // ADDRESS
    // =====================================================

    private AddressXml buildAddress(
            SuppliersRowEntity supplier,
            String name20,
            TaxInfo tax
    ) {
        return AddressXml.builder()
                .addressStreet1(supplier.getStreetName())
                .addressStreet2(supplier.getStreetName2())
                .addressStreet3(supplier.getStreetName3())
                .addressZip(supplier.getZipCode())
                .addressCity(supplier.getCityCode())
                .addressCityCode("") // OC va vacío
                .addressName(supplier.getStreetName())
                .addressSearchName(name20)
                .addressTelephone("")
                .addressEMail("")
                .addressFormat(XMLConstants.CERO)
                .addressIsTemporary("false")
                .txzTaxZone(tax.txzTaxZone())
                .txclTaxCls(tax.txclTaxCls())
                .addressIsSendToPostal("false")
                .addressIsTaxable("false")
                .addressIsTaxInCity("false")
                .addressIsTaxIncluded("false")
                .addressTaxIDFederal(supplier.getRfc())
                .addressTaxIDState(supplier.getRfc())
                .addressTaxDeclaration(XMLConstants.CERO)
                .addressLogicKeyString(XMLConstants.ADDRESS_LOGIC_KEY)
                .tcStateCode(supplier.getStateCode())
                .tcCountryCode(supplier.getCountryCode())
                .tcAddressTypeCode("HEADOFFICE")
                .tcLngCode(XMLConstants.LANG_CODE)
                .tcStateDescription(supplier.getStateDescription())
                .tcCountryDescription("MEXICO")
                .tiCountryFormat(XMLConstants.CERO)
                .tcLngDescription("latin spanish")
                .lastModifiedDate(XMLConstants.OC_LAST_MODIFIED_DATE)
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.ROW_ID)
                .tc_ParentRowid(XMLConstants.PARENT_ROW_ID)
                .build();
    }

    // =====================================================
    // CONTACT
    // =====================================================

    private ContactXml buildContact(SuppliersRowEntity supplier) {
        return ContactXml.builder()
                .contactFunction("")
                .contactName(supplier.getContactName())
                .contactGender(XMLConstants.CONTACT_MALE)
                .contactEmail(supplier.getContactEmail())
                .contactIsPrimary("true")
                .contactIsSecondary("false")
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate(XMLConstants.OC_LAST_MODIFIED_DATE)
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.CONTACT_ROW_ID)
                .tc_ParentRowid(XMLConstants.ROW_ID)
                .build();
    }

    // =====================================================
    //(GLs)
    // =====================================================

    private CreditorNodoXML buildCreditor(
            SuppliersRowEntity supplier,
            TaxInfo tax
    ) {
        boolean isForeign =
                supplier.getCountryCode() != null &&
                !"MX".equalsIgnoreCase(supplier.getCountryCode());

        String currency = supplier.getSupplierCurrency();

        CesarQadRules.GlProfiles gl =
                CesarQadRules.resolveGlProfiles(
                        Domain.RFCORPO,
                        currency,
                        isForeign,
                        false
                );

        String paymentTerm =
                CesarQadRules.resolvePaymentTerms(Domain.RFCORPO, null);

        return CreditorNodoXML.builder()
                .creditorIsActive("false")
                .creditorCode(supplier.getErpIdQad())
                .vatDeliveryType("PRODUCT")
                .vatPercentageLevel("NONE")
                .creditorIsSendRemittance("false")
                .creditorIsIndividualPaymnt("false")
                .creditorIsTaxable("false")
                .creditorIsTaxInCity("false")
                .creditorIsTaxIncluded("false")
                .creditorTaxIDFederal(supplier.getRfc())
                .creditorTaxIDState(supplier.getRfc())
                .creditorTaxDeclaration(XMLConstants.CERO)
                .creditorIsTaxReport("false")
                .creditorIsTaxConfirmed("false")
                .creditorIsWHT("false")
                .creditorIsBearBankCharge("false")
                .tlBusinessRelationIsInterco("false")
                .txzTaxZone(tax.txzTaxZone())
                .txclTaxCls(tax.txclTaxCls())


                //  GLs
                .tcInvControlGLProfileCode(gl.invControl())
                .tcCnControlGLProfileCode(gl.cnControl())
                .tcPrepayControlGLProfileCode(gl.prepayControl())
                .tcDivisionProfileCode(gl.divProfile())
                .tcPurchaseGLProfileCode(gl.purchaseGlProfile())

                .tcNormalPaymentConditionCode(paymentTerm)
                .tcNormalPaymentConditionType("NORMAL")
                .tcReasonCode("INV TO APPROVE")
                .tcBusinessRelationCode(supplier.getErpIdQad())
                .tcBusinessRelationName1(supplier.getSupplierName())
                .tcCurrencyCode(currency)

                .lastModifiedDate(XMLConstants.OC_LAST_MODIFIED_DATE)
                .lastModifiedTime(XMLConstants.LAST_MODIFIED_TIME)
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.ROW_ID)
                .build();
    }

    // =====================================================
    // TAX
    // =====================================================

    private TaxInfo resolveTaxInfo(
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp
    ) {
        String txz =
                (taxZoneFromErp != null && !taxZoneFromErp.isEmpty()
                        && taxZoneFromErp.get(0) != null
                        && !taxZoneFromErp.get(0).isBlank())
                        ? taxZoneFromErp.get(0)
                        : "MX";

        String txcl =
                (taxClassFromErp != null && !taxClassFromErp.isBlank())
                        ? taxClassFromErp
                        : catalogService.resolveTaxClass(erpId, taxClassFromErp);

        return new TaxInfo(txz, txcl);
    }

    private String left(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len);
    }

    public record TaxInfo(String txzTaxZone, String txclTaxCls) {}
}