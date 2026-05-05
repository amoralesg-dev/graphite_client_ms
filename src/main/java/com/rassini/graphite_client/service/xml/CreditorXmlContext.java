package com.rassini.graphite_client.service.xml;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditorXmlContext {

    // ===== Output =====
    private String outputFileName;

    // ===== ContextInfo =====
    private String tcCompanyCode;
    private String lastModifiedDate;
    private String lastModifiedTime;
    private String lastModifiedUser;

    // ===== Creditor =====
    private String creditorCode;
    private String tcCurrencyCode;
    private String tcNormalPaymentConditionCode;

    // ===== GL Profiles =====
    private String tcInvControlGLProfileCode;
    private String tcCnControlGLProfileCode;
    private String tcPrepayControlGLProfileCode;
    private String tcDivisionProfileCode;
}
