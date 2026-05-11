package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BusinessRelationXml {

    private String businessRelationCode;
    private String businessRelationName1;
    private String businessRelationName2;
    private String businessRelationName3;
    private String businessRelationSearchName;
    private String businessRelationICCode;

    private String businessRelationIsActive;
    private String businessRelationIsInterco;
    private String businessRelationIsInComp;
    private String businessRelationIsCompens;
    private String businessRelationIsTaxRep;
    private String businessRelationIsLastFill;
    private String businessRelationIsDomRestr;

    private String businessRelationAVRCode;
    private String businessRelationEANCode;
    private String businessRelationNameCtrl;
    private String businessRelationOrgType;
    private String businessRelationComRegisterNbr;
    private String businessRelationInstNbr;
    private String businessRelationInstCode;

    private String tcCorporateGroupCode;
    private String tcLngCode;
    private String tcSalesPriceListCode;
    private String tcPurchasePriceListCode;
    private String tcCostPriceListCode;

    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;
    private String tc_Rowid;
    private String tc_ParentRowid;
}