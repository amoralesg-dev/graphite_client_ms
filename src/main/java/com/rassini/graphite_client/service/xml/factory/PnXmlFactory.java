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
        String name20 = left(supplier.getBusinessRelationName1(), 20);

        TaxInfo tax = resolveTaxInfoPn(erpId, taxClassFromErp);

        return XmlContext.builder()
                .outputFileName("RPIEDRAS_busrel_" + supplier.getErpIDQAD() + ".xml")
                .contextInfo(buildContextInfoBusrel(supplier))
                .businessRelation(buildBusinessRelation(supplier))
                .address(buildAddress(supplier, name20, tax))
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
                .outputFileName("RPIEDRAS_creditor_" + supplier.getErpIDQAD() + ".xml")
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
                .tiPriority(XMLConstants.CERO)
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
                .tiRequestStartTime(XMLConstants.CERO)
                .tcCBFVersion(XMLConstants.CONTEXT_VERSION)
                .tcActivityCode("Create")
                .tlPartialUpdate("false")
                .build();
    }

    // =====================================================
    // NODES
    // =====================================================
    private BusinessRelationXml buildBusinessRelation(SuppliersRowEntity supplier) {

        String name20 = left(supplier.getBusinessRelationName1(), 20);

        return BusinessRelationXml.builder()
                .businessRelationCode("PR" + supplier.getErpIDQAD())
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
            TaxInfo tax
    ) {

        String currency = supplier.getCurrency();

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
                .tcBusinessRelationCode("PR"+supplier.getErpIDQAD())
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
    private TaxInfo resolveTaxInfoPn(String erpId, String taxClassFromErp) {

        String txz = "MX";

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