package com.rassini.graphite_client.service.xml.factory;



import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.context.*;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules;
import com.rassini.graphite_client.service.xml.impl.util.CesarQadRules.Domain;
import com.rassini.graphite_client.service.xml.impl.util.DateUtil;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

/**
 * Factory PN (PIEDRAS NEGRAS – ERP 09)
 *  Un build por nodo
 *  Creditor incluido
 *  Sin lógica en el service
 */
public class PnXmlFactory {

    private final CatalogService catalogService;

    public PnXmlFactory(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // =====================================================
    // PUBLIC: BUSREL CONTEXT
    // =====================================================
    public XmlContext buildBusrelContext(
            SuppliersRowEntity supplier,
            String taxClassFromErp
    ) {

        String erpId = "09";
        String name20 = left(supplier.getSupplierName(), 20);
        String name36 = left(supplier.getSupplierName(), 36);   

        TaxInfo tax = resolveTaxInfoPn(erpId, taxClassFromErp);

        return XmlContext.builder()
                .outputFileName("RPIEDRAS_busrel_" + supplier.getErpIdQad() + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier))
                .address(buildAddress(supplier, name20,name36, tax))
                .contact(buildContact(supplier))
                .build();
    }

    // =====================================================
    // PUBLIC: CREDITOR CONTEXT
    // =====================================================
    public CreditorXmlContext buildCreditorContext(
            SuppliersRowEntity supplier,
            String taxClassFromErp
    ) {

        String erpId = "09";
        TaxInfo tax = resolveTaxInfoPn(erpId, taxClassFromErp);

        return CreditorXmlContext.builder()
                .outputFileName("RPIEDRAS_creditor_" + supplier.getErpIdQad() + ".xml")
                .contextInfo(buildContextInfoCreditor(erpId, supplier))
                .creditor(buildCreditor(supplier, tax))
                .build();
    }

    // =====================================================
    // CONTEXT INFO
    // =====================================================
    private ContextInfoXml buildContextInfoBusrel(SuppliersRowEntity supplier) {
        return ContextInfoXml.builder()
                .tcCompanyCode(supplier.getBusinessUnitCode())

                .tiPriority(XMLConstants.CERO)
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode(catalogService.getActivityCode(supplier))
                .tlPartialUpdate((XMLConstants.FALSE))
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
                .tlPartialUpdate((XMLConstants.FALSE))
                .build();
    }

    

    // =====================================================
    // NODES
    // =====================================================
    private BusinessRelationXml buildBusinessRelation(SuppliersRowEntity supplier) {

        String name20 = left(supplier.getSupplierName(), 20);

        return BusinessRelationXml.builder()
                .businessRelationCode("PR" + supplier.getErpIdQad())
                .businessRelationName1(supplier.getSupplierName())
                .businessRelationName2(supplier.getSupplierName())
                .businessRelationName3(supplier.getSupplierName())
                .businessRelationSearchName(name20)
                .businessRelationIsActive(XMLConstants.TRUE)
                .businessRelationIsInterco((XMLConstants.FALSE))
                .businessRelationIsInComp((XMLConstants.FALSE))
                .businessRelationIsCompens(XMLConstants.TRUE)
                .businessRelationIsTaxRep((XMLConstants.FALSE))
                .businessRelationIsLastFill((XMLConstants.FALSE))
                .businessRelationIsDomRestr((XMLConstants.FALSE))
                .tcCorporateGroupCode(XMLConstants.PROVEEDOR)
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
        String streetName36 = left(supplier.getStreetName(), 36);
        return AddressXml.builder()
                .addressStreet1(streetName36)
                .addressStreet2(supplier.getStreetName2())
                .addressStreet3(supplier.getStreetName3())
                .addressZip(supplier.getZipCode())
                .addressCity(supplier.getCityCode())
                .addressCityCode(supplier.getCityCode())
                .addressName(streetName36)
                .addressSearchName(name20)
                .addressTelephone("")
                .addressEMail("")
                .addressFormat(XMLConstants.CERO)
                .addressIsTemporary((XMLConstants.FALSE))
                .txzTaxZone(tax.txzTaxZone())
                .txclTaxCls(tax.txclTaxCls())
                .addressIsSendToPostal((XMLConstants.FALSE))
                .addressIsTaxable((XMLConstants.FALSE))
                .addressIsTaxInCity((XMLConstants.FALSE))
                .addressIsTaxIncluded((XMLConstants.FALSE))
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
                .contactIsSecondary((XMLConstants.FALSE))
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
            TaxInfo tax
    ) {

        String currency = supplier.getSupplierCurrency();

        CesarQadRules.GlProfiles gl =
                CesarQadRules.resolveGlProfiles(
                        Domain.RPIEDRAS,
                        currency,
                        false,
                        false
                );

        String paymentTerm =
                CesarQadRules.resolvePaymentTerms(
                        Domain.RPIEDRAS,
                        "30 DIAS P/FACTURA"
                );

        return CreditorNodoXML.builder()

                .creditorIsActive(XMLConstants.TRUE)
                .creditorCode(supplier.getErpIdQad())
                .vatDeliveryType("PRODUCT")
                .vatPercentageLevel("NONE")
                .creditorIsSendRemittance((XMLConstants.FALSE))
                .creditorIsIndividualPaymnt((XMLConstants.FALSE))
                .creditorIsTaxable((XMLConstants.FALSE))
                .creditorIsTaxInCity((XMLConstants.FALSE))
                .creditorIsTaxIncluded((XMLConstants.FALSE))

                .creditorTaxIDFederal(supplier.getRfc())
                .creditorTaxIDState(supplier.getRfc())
                .creditorTaxDeclaration(XMLConstants.CERO)

                .creditorIsTaxReport((XMLConstants.FALSE))
                .creditorIsTaxConfirmed((XMLConstants.FALSE))
                .creditorIsWHT((XMLConstants.FALSE))
                .creditorIsBearBankCharge((XMLConstants.FALSE))
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

                .tcReasonCode("INICIAL")
                .tlBusinessRelationIsInterco((XMLConstants.FALSE))
                .tcBusinessRelationCode("PR"+supplier.getErpIdQad())
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
    private TaxInfo resolveTaxInfoPn(String erpId, String taxClassFromErp) {

        String txz = "MEX";

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