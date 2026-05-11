package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContextInfoXml {

    private String tcCompanyCode;
    private String tcAction;
    private String tiPriority;
    private String ttRequestStartDate;
    private String tiRequestStartTime;
    private String tcComment;
    private String tcCBFVersion;
    private String tcComponentVersion;
    private String tcActivityCode;
    private String tlPartialUpdate;
    private String tcPartialUpdateExceptionList;

    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;
    private String tc_Rowid;
    private String tc_ParentRowid;
}
