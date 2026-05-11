package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressXml {

    private String addressStreet1;
    private String addressStreet2;
    private String addressStreet3;
    private String addressZip;
    private String addressCity;
    private String addressCityCode;
    private String addressName;
    private String addressSearchName;
    private String addressTelephone;
    private String addressEMail;
    private String addressWebSite;
    private String addressFax;
    private String addressFormat;
    private String addressIsTemporary;

    private String txzTaxZone;
    private String txclTaxCls;
    private String txuTaxUsage;

    private String addressPostalAddress1;
    private String addressPostalAddress2;
    private String addressIsSendToPostal;

    private String addressState;
    private String addressPostalZip;
    private String addressPostalCity;
    private String addressCounty;

    private String addressIsTaxable;
    private String addressIsTaxInCity;
    private String addressIsTaxIncluded;

    private String addressTaxIDFederal;
    private String addressTaxIDState;
    private String addressTaxIDMisc1;
    private String addressTaxIDMisc2;
    private String addressTaxIDMisc3;
    private String addressTaxDeclaration;

    private String addressLogicKeyString;
    private String addressExternalValidationCode;

    private String tcCountyCode;
    private String tcStateCode;
    private String tcCountryCode;
    private String tcAddressTypeCode;
    private String tcLngCode;

    private String tcStateDescription;
    private String tcCountyDescription;
    private String tcCountryDescription;
    private String tiCountryFormat;
    private String tcLngDescription;
    private String tcCoCNumber;

    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;
    private String tc_Rowid;
    private String tc_ParentRowid;
}