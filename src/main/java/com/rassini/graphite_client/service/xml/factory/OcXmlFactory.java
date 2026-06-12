package com.rassini.graphite_client.service.xml.factory;

import java.util.List;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
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
        String name36 = left(supplier.getSupplierName(), 36);

        return XmlContext.builder()
                .outputFileName("busrel_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier, name20, name36))
                .address(buildAddress(supplier, name20, name36, tax))
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
            List<String> taxZoneFromErp,
            String paymentTerms
    ) {
        TaxInfo tax = resolveTaxInfo(erpId, taxClassFromErp, taxZoneFromErp);

        return CreditorXmlContext.builder()
                .outputFileName("creditor_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoCreditor(erpId, supplier))
                .creditor(buildCreditor(supplier, tax, paymentTerms))
                .build();
    }

    // =====================================================
    // CONTEXT INFO
    // =====================================================

    private ContextInfoXml buildContextInfoBusrel(SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())
                .tcAction(catalogService.getAction(supplier))
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate(XMLConstants.NULL)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcComment("")
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcComponentVersion("")
                .tcActivityCode(catalogService.getActivityCode(supplier))
                .tlPartialUpdate(XMLConstants.FALSE)
                .tcPartialUpdateExceptionList("")
                .build();
    }

    private ContextInfoXml buildContextInfoCreditor(String erpId, SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(erpId)
                .tcAction(catalogService.getAction(supplier))
                .tiPriority(XMLConstants.CERO)
                .ttRequestStartDate(XMLConstants.NULL)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode(catalogService.getActivityCode(supplier))
                .tlPartialUpdate(XMLConstants.FALSE)
                .build();
    }

    // =====================================================
    // BUSINESS RELATION
    // =====================================================

    private BusinessRelationXml buildBusinessRelation(
            SuppliersRowEntity supplier,
            String name20,String name36
    ) {
        return BusinessRelationXml.builder()
                .businessRelationCode(supplier.getErpIdQad())
                .businessRelationName1(name36)
                .businessRelationName2(name36)
                .businessRelationName3(name36)
                .businessRelationSearchName(name20)
                .businessRelationIsActive(XMLConstants.TRUE)
                .businessRelationIsInterco(XMLConstants.FALSE)
                .businessRelationIsInComp(XMLConstants.FALSE)
                .businessRelationIsCompens(XMLConstants.TRUE)
                .businessRelationIsTaxRep(XMLConstants.FALSE)
                .businessRelationIsLastFill(XMLConstants.FALSE)
                .businessRelationIsDomRestr(XMLConstants.FALSE)
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
            String name36,
            TaxInfo tax
    ) {
        return AddressXml.builder()
                .addressStreet1(supplier.getStreetName())
                .addressStreet2(supplier.getStreetName2())
                .addressStreet3(supplier.getStreetName3())
                .addressZip(supplier.getZipCode())
                .addressCity(supplier.getCityCode())
                .addressCityCode("") // OC va vacío
                .addressName(name36)
                .addressSearchName(name20)
                .addressTelephone("")
                .addressEMail("")
                .addressFormat(XMLConstants.CERO)
                .addressIsTemporary(XMLConstants.FALSE)
                .txzTaxZone(tax.txzTaxZone())
                .txclTaxCls(tax.txclTaxCls())
                .addressIsSendToPostal(XMLConstants.FALSE)
                .addressIsTaxable(XMLConstants.FALSE)
                .addressIsTaxInCity(XMLConstants.FALSE)
                .addressIsTaxIncluded(XMLConstants.FALSE)
                .addressTaxIDFederal(supplier.getRfc())
                .addressTaxIDState(supplier.getRfc())
                .addressTaxDeclaration(XMLConstants.CERO)
                .addressLogicKeyString(XMLConstants.ADDRESS_LOGIC_KEY)
                .tcStateCode(supplier.getStateCode())
                .tcCountryCode(supplier.getCountryCode())
                .tcAddressTypeCode("HEADOFFICE")
                .tcLngCode(XMLConstants.LANG_CODE)
                .tcStateDescription(supplier.getStateDescription())
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
                .contactIsPrimary(XMLConstants.TRUE)
                .contactIsSecondary(XMLConstants.FALSE)
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
            TaxInfo tax,
            String  paymentTerms
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
                CesarQadRules.resolvePaymentTerms(Domain.RFCORPO, paymentTerms);

        return CreditorNodoXML.builder()
                .creditorIsActive(XMLConstants.TRUE)
                .creditorCode(supplier.getErpIdQad())
                .vatDeliveryType("PRODUCT")
                .vatPercentageLevel("NONE")
                .creditorIsSendRemittance(XMLConstants.FALSE)
                .creditorIsIndividualPaymnt(XMLConstants.FALSE)
                .creditorIsTaxable(XMLConstants.FALSE)
                .creditorIsTaxInCity(XMLConstants.FALSE)
                .creditorIsTaxIncluded(XMLConstants.FALSE)
                .creditorTaxIDFederal(supplier.getRfc())
                .creditorTaxIDState(supplier.getRfc())
                .tcCreditorTypeCode(supplier.getSupplierTypeCode())
                .tcPurchaseTypeCode(supplier.getPurchaseTypeCode())
                .creditorTaxDeclaration(XMLConstants.CERO)
                .creditorIsTaxReport(XMLConstants.FALSE)
                .creditorIsTaxConfirmed(XMLConstants.FALSE)
                .creditorIsWHT(XMLConstants.FALSE)
                .creditorIsBearBankCharge(XMLConstants.FALSE)
                .tlBusinessRelationIsInterco(XMLConstants.FALSE)
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