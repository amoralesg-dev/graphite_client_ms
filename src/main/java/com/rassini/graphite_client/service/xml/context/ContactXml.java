package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactXml {

    private String contactFunction;
    private String contactName;
    private String contactInitials;
    private String contactGender;
    private String contactTitle;
    private String contactTelephone;
    private String contactMobilePhone;
    private String contactEmail;
    private String contactFax;
    private String contactIsPrimary;
    private String contactIsSecondary;

    private String tcLngCode;

    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;
    private String tc_Rowid;
    private String tc_ParentRowid;
}