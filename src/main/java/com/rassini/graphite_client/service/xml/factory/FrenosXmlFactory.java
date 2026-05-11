package com.rassini.graphite_client.service.xml.factory;

import java.util.List;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.context.*;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules.Domain;
import com.rassini.graphite_client.service.xml.impl.util.DateUtil;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

/**
 * Factory FRENOS (ERP 1000)
 * - Un build por nodo
 * - Creditor incluido
 * - Reglas César aplicadas
 */
public class FrenosXmlFactory {

    private final CatalogService catalogService;

    public FrenosXmlFactory(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // =====================================================
    // PUBLIC: BUSREL CONTEXT
    // =====================================================
    public XmlContext buildBusrelContext(
            SuppliersRowEntity supplier,
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp
    ) {

        TaxInfo tax = resolveTaxInfoFrenos(erpId, taxClassFromErp, taxZoneFromErp);
        String name20 = left(supplier.getBusinessRelationName1(), 20);

        return XmlContext.builder()
                .outputFileName("busrel_" + supplier.getErpIDQAD() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier, name20))
                .address(buildAddress(supplier, name20, tax))
                .contact(buildContact(supplier))
                .build();
    }

    // =====================================================
    // PUBLIC: CREDITOR CONTEXT
    // =====================================================
    public CreditorXmlContext buildCreditorContext(
            SuppliersRowEntity supplier,
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp,
            String paymentTermsFromErp
    ) {

        TaxInfo tax = resolveTaxInfoFrenos(erpId, taxClassFromErp, taxZoneFromErp);

        return CreditorXmlContext.builder()
                .outputFileName("creditor_" + supplier.getCreditorCode() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoCreditor(erpId))
                .creditor(buildCreditor(supplier, tax, paymentTermsFromErp))
                .build();
    }

    // =====================================================
    // CONTEXT INFO
    // =====================================================
    private ContextInfoXml buildContextInfoBusrel(SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate(XMLConstants.NULL)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
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
    // NODES
    // =====================================================
    private BusinessRelationXml buildBusinessRelation(
            SuppliersRowEntity supplier,
            String name20
    ) {
        return BusinessRelationXml.builder()
                .businessRelationCode(supplier.getErpIDQAD())
                .businessRelationName1(supplier.getBusinessRelationName1())
                .businessRelationName2(supplier.getBusinessRelationName1())
                .businessRelationName3(supplier.getBusinessRelationName1())
                .businessRelationSearchName(name20)
                .businessRelationIsActive("true")
                .businessRelationIsInterco("false")
                .businessRelationIsInComp("false")
                .businessRelationIsCompens("true")
                .businessRelationIsTaxRep("false")
                .businessRelationIsLastFill("false")
                .businessRelationIsDomRestr("false")
                .tcCorporateGroupCode(XMLConstants.PROVEEDOR)
                .tcLngCode("Is")
                .lastModifiedDate(DateUtil.todayYyyyMD())
                .lastModifiedTime(DateUtil.nowHhMmSs())
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.PARENT_ROW_ID)
                .build();
    }

    private AddressXml buildAddress(
            SuppliersRowEntity supplier,
            String name20,
            TaxInfo tax
    ) {
        return AddressXml.builder()
                .addressStreet1(supplier.getAddressStreet1())
                .addressStreet2(supplier.getAddressStreet2())
                .addressStreet3(supplier.getAddressStreet3())
                .addressZip(supplier.getAddressZip())
                .addressCity(supplier.getCityCode())
                .addressCityCode(supplier.getCityCode())
                .addressName(supplier.getAddressStreet1())
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
                .lastModifiedDate(DateUtil.todayYyyyMD())
                .lastModifiedTime(DateUtil.nowHhMmSs())
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.ROW_ID)
                .tc_ParentRowid(XMLConstants.PARENT_ROW_ID)
                .build();
    }

    private ContactXml buildContact(SuppliersRowEntity supplier) {
        return ContactXml.builder()
                .contactFunction("")
                .contactName(supplier.getContactName())
                .contactGender(XMLConstants.CONTACT_MALE)
                .contactEmail(supplier.getContactEmail())
                .contactIsPrimary("true")
                .contactIsSecondary("false")
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate(DateUtil.todayYyyyMD())
                .lastModifiedTime(DateUtil.nowHhMmSs())
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.CONTACT_ROW_ID)
                .tc_ParentRowid(XMLConstants.ROW_ID)
                .build();
    }

    private CreditorNodoXML buildCreditor(
            SuppliersRowEntity supplier,
            TaxInfo tax,
            String paymentTermsFromErp
    ) {

        boolean isForeign =
                supplier.getCountryCode() != null &&
                !"MX".equalsIgnoreCase(supplier.getCountryCode());

        String currency = supplier.getCurrency();

        CesarQadRules.GlProfiles gl =
                CesarQadRules.resolveGlProfiles(
                        Domain.RFRENOS,
                        currency,
                        isForeign,
                        false
                );

        String paymentTerm =
                (paymentTermsFromErp != null && !paymentTermsFromErp.isBlank())
                        ? paymentTermsFromErp
                        : "30";

        return CreditorNodoXML.builder()

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

                .creditorBirthDate(XMLConstants.NULL)

                .txzTaxZone(tax.txzTaxZone())
                .txclTaxCls(tax.txclTaxCls())

                .tcNormalPaymentConditionCode(paymentTerm)
                .tcNormalPaymentConditionType("NORMAL")

                .tcInvControlGLProfileCode(gl.invControl())
                .tcCnControlGLProfileCode(gl.cnControl())
                .tcPrepayControlGLProfileCode(gl.prepayControl())
                .tcDivisionProfileCode(gl.divProfile())
                .tcPurchaseGLProfileCode(gl.purchaseGlProfile())

                .tcReasonCode("INV TO APPROVE")
                .tlBusinessRelationIsInterco("false")
                .tcBusinessRelationCode(supplier.getErpIDQAD())
                .tcBusinessRelationName1(supplier.getBusinessRelationName1())
                .tcCurrencyCode(currency)

                .tcCreditorTypeCode(supplier.getSupplierType())
                .tcPurchaseTypeCode(supplier.getPurchaseTypeCode())

                .lastModifiedDate(DateUtil.todayYyyyMD())
                .lastModifiedTime(DateUtil.nowHhMmSs())
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)

                .qADT01(XMLConstants.NULL)
                .qADD01(XMLConstants.CERO)
                .tc_Rowid(XMLConstants.ROW_ID)

                .build();
    }

    // =====================================================
    // TAX
    // =====================================================
    private TaxInfo resolveTaxInfoFrenos(
            String erpId,
            String taxClassFromErp,
            List<String> taxZoneFromErp
    ) {

        String txz = null;

        if (taxZoneFromErp != null && !taxZoneFromErp.isEmpty()
                && taxZoneFromErp.get(0) != null && !taxZoneFromErp.get(0).isBlank()) {
            txz = taxZoneFromErp.get(0);
        }

        String txcl =
                (taxClassFromErp != null && !taxClassFromErp.isBlank())
                        ? taxClassFromErp
                        : catalogService.resolveTaxClass(erpId, taxClassFromErp);

        return new TaxInfo(txz, txcl);
    }

    // =====================================================
    // Helpers
    // =====================================================
    private String left(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len);
    }

    private record TaxInfo(String txzTaxZone, String txclTaxCls) {}
}