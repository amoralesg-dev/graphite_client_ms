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
        String name20 = left(supplier.getSupplierName(), 20);
        String name36 = left(supplier.getSupplierName(), 36);

        return XmlContext.builder()
                .outputFileName("busrel_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier, name20, name36))
                .address(buildAddress(supplier, name20,name36, tax))
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
                .outputFileName("creditor_" + supplier.getErpIdQad() + "_" + erpId + ".xml")
                .contextInfo(buildContextInfoCreditor(erpId, supplier))
                .creditor(buildCreditor(supplier, tax, paymentTermsFromErp))
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
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode(catalogService.getActivityCode(supplier))
                .tlPartialUpdate(catalogService.getPartialUpdate(supplier))
                .build();
    }

    private ContextInfoXml buildContextInfoCreditor(String erpId, SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(erpId)
                .tcAction(catalogService.getAction(supplier))
                .tiPriority(XMLConstants.CERO)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode(catalogService.getActivityCode(supplier))
                .tlPartialUpdate(catalogService.getPartialUpdate(supplier))
                .build();
    }

    // =====================================================
    // NODES
    // =====================================================
    private BusinessRelationXml buildBusinessRelation(
            SuppliersRowEntity supplier,
            String name20, String name36
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
                .tcLngCode(XMLConstants.LANG_CODE)
                .lastModifiedDate(DateUtil.todayYyyyMD())
                .lastModifiedTime(DateUtil.nowHhMmSs())
                .lastModifiedUser(XMLConstants.LAST_MODIFIED_USER)
                .tc_Rowid(XMLConstants.PARENT_ROW_ID)
                .build();
    }

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
                .addressCityCode(supplier.getCityCode())
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
                .contactIsPrimary(XMLConstants.TRUE)
                .contactIsSecondary(XMLConstants.FALSE)
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

        String currency = supplier.getSupplierCurrency();

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
                .creditorTaxDeclaration(XMLConstants.CERO)

                .creditorIsTaxReport(XMLConstants.FALSE)
                .creditorIsTaxConfirmed(XMLConstants.FALSE)
                .creditorIsWHT(XMLConstants.FALSE)
                .creditorIsBearBankCharge(XMLConstants.FALSE)

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

                .tcReasonCode("RECIBO-PENDIENTE")
                .tlBusinessRelationIsInterco(XMLConstants.FALSE)
                .tcBusinessRelationCode(supplier.getErpIdQad())
                .tcBusinessRelationName1(supplier.getSupplierName())
                .tcCurrencyCode(currency)

                .tcCreditorTypeCode(supplier.getSupplierTypeCode())
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

        String txz = "MEX";

       /*if (taxZoneFromErp != null && !taxZoneFromErp.isEmpty()
                && taxZoneFromErp.get(0) != null && !taxZoneFromErp.get(0).isBlank()) {
            txz = taxZoneFromErp.get(0);
        }*/

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