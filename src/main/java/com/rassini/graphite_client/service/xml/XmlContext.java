package com.rassini.graphite_client.service.xml;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class XmlContext {

    // ===== output =====
    private String outputFileName;

    // ===== ContextInfo =====
    private String tcCompanyCode;
    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;

    // ===== BusinessRelation =====
    private String businessRelationCode;
    private String entityName20;
    private String tcCorporateGroupCode;

    // ===== Address =====
    private String addressStreet1;
    private String addressStreet2;
    private String addressStreet3;
    private String addressZip;
    private String addressCity;
    private String addressName;
    private String addressSearchName;
    private String addressEmail;

    private String txzTaxZone;
    private String txclTaxCls;

    private String rfc;
    private String rfcState;

    private String tcStateCode;
    private String tcCountryCode;
    private String tcStateDescription;
    private String tcCountryDescription;

    // ===== Contact =====
    private String contactName;
    private String contactEmail;
}
