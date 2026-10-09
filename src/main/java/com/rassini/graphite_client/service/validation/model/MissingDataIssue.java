package com.rassini.graphite_client.service.validation.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MissingDataIssue {

    private String connectionId;
    private String supplierCode;
    private String erpIdQad;
    private String legacyMappedErpId;
    private String supplierName;
    private String businessUnitCode;
    private String plantCode;
    private String maskedAccountNumber;

    private OutputType outputType;
    private String subType; // busrel, creditor, sync_file

    private String fieldName;
    private String expectedNode;
    private String alternateNodeFound;
    private String receivedValue;

    private IssueType issueType;
    private IssueSeverity severity;
    private String technicalMessage;
    private String rootCauseException;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String generatedFilePath;
    private OutputResult result;
}
