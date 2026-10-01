package com.rassini.graphite_client.service.validation.collector;

import com.rassini.graphite_client.service.validation.model.MissingDataIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class MissingDataCollector {

    private final ThreadLocal<List<MissingDataIssue>> currentThreadIssues = ThreadLocal.withInitial(ArrayList::new);

    public void clear() {
        currentThreadIssues.get().clear();
    }

    public void recordIssue(MissingDataIssue issue) {
        if (issue == null) return;
        currentThreadIssues.get().add(issue);

        log.warn("[MISSING-DATA] supplier={} erpId={} bu={} account={} outputType={} subType={} field={} severity={} result={} msg='{}'",
                issue.getSupplierCode(),
                issue.getErpIdQad(),
                issue.getBusinessUnitCode(),
                issue.getMaskedAccountNumber() != null ? issue.getMaskedAccountNumber() : "N/A",
                issue.getOutputType(),
                issue.getSubType(),
                issue.getFieldName(),
                issue.getSeverity(),
                issue.getResult(),
                issue.getTechnicalMessage());
    }

    public List<MissingDataIssue> getIssues() {
        return Collections.unmodifiableList(new ArrayList<>(currentThreadIssues.get()));
    }

    public boolean hasIssues() {
        return !currentThreadIssues.get().isEmpty();
    }

    public boolean hasBlockingIssues(String supplierCode, String businessUnit, String subType) {
        return currentThreadIssues.get().stream()
                .filter(i -> supplierCode == null || supplierCode.equals(i.getSupplierCode()))
                .filter(i -> businessUnit == null || businessUnit.equals(i.getBusinessUnitCode()))
                .filter(i -> subType == null || subType.equalsIgnoreCase(i.getSubType()))
                .anyMatch(i -> i.getSeverity() == com.rassini.graphite_client.service.validation.model.IssueSeverity.BLOCKING);
    }

    public boolean hasWarningIssues(String supplierCode, String businessUnit, String subType) {
        return currentThreadIssues.get().stream()
                .filter(i -> supplierCode == null || supplierCode.equals(i.getSupplierCode()))
                .filter(i -> businessUnit == null || businessUnit.equals(i.getBusinessUnitCode()))
                .filter(i -> subType == null || subType.equalsIgnoreCase(i.getSubType()))
                .anyMatch(i -> i.getSeverity() == com.rassini.graphite_client.service.validation.model.IssueSeverity.WARNING);
    }

    public boolean hasBlockingIssues(String businessUnit, String subType) {
        return hasBlockingIssues(null, businessUnit, subType);
    }

    public boolean hasWarningIssues(String businessUnit, String subType) {
        return hasWarningIssues(null, businessUnit, subType);
    }
}
