package com.rassini.graphite_client.service.validation.service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.validation.collector.MissingDataCollector;
import com.rassini.graphite_client.service.validation.model.IssueSeverity;
import com.rassini.graphite_client.service.validation.model.IssueType;
import com.rassini.graphite_client.service.validation.model.MissingDataIssue;
import com.rassini.graphite_client.service.validation.model.OutputResult;
import com.rassini.graphite_client.service.validation.model.OutputType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutputValidationService {

    private final MissingDataCollector collector;

    public boolean validateBusrel(SuppliersRowEntity supplier, String businessUnit) {
        boolean valid = true;
        String supplierCode = supplier != null ? supplier.getSupplierCode() : null;
        String erpIdQad = supplier != null ? supplier.getErpIdQad() : null;

        if (supplier == null) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("supplier_row")
                    .issueType(IssueType.REQUIRED_FIELD_MISSING)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Fila de proveedor no encontrada en base de datos para BU=" + businessUnit)
                    .build());
            return false;
        }

        // Validación de campos bloqueantes para Business Relation
        if (isBlank(supplier.getSupplierName())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("supplierName")
                    .expectedNode("Entity_Name / Entity_Name_Translations")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Nombre de proveedor nulo o vacío")
                    .build());
            valid = false;
        }

        if (isBlank(supplier.getRfc())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("rfc")
                    .expectedNode("Integration_Tax_ID")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Tax ID / RFC nulo o vacío")
                    .build());
            valid = false;
        }

        if (isBlank(supplier.getStateCode())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("stateCode")
                    .expectedNode("Address.Address_Region_State")
                    .issueType(IssueType.CATALOG_EQUIVALENCE_MISSING)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Equivalencia de estado no encontrada o stateCode nulo")
                    .build());
            valid = false;
        }

        if (isBlank(supplier.getCountryCode())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("countryCode")
                    .expectedNode("Address.Address_Country")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Código de país nulo o vacío")
                    .build());
            valid = false;
        }

        // Validaciones no bloqueantes (Warnings -> dirigen a Manual si no hay bloqueante)
        if (isBlank(supplier.getStreetName())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("streetName")
                    .expectedNode("Address.Address_1")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Calle nula o vacía en dirección de proveedor")
                    .build());
        }

        if (isBlank(supplier.getZipCode())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("busrel")
                    .fieldName("zipCode")
                    .expectedNode("Address.Address_Postal_Code")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Código postal nulo o vacío")
                    .build());
        }

        return valid;
    }

    public boolean validateCreditor(SuppliersRowEntity supplier, String businessUnit) {
        boolean valid = true;
        String supplierCode = supplier != null ? supplier.getSupplierCode() : null;
        String erpIdQad = supplier != null ? supplier.getErpIdQad() : null;

        if (supplier == null) return false;

        if (isBlank(supplier.getSupplierCurrency())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("creditor")
                    .fieldName("supplierCurrency")
                    .expectedNode("Bank_Currency_List[0]")
                    .issueType(IssueType.CATALOG_EQUIVALENCE_MISSING)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Moneda de proveedor no encontrada o nula")
                    .build());
            valid = false;
        }

        if (isBlank(supplier.getSupplierTypeCode())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("creditor")
                    .fieldName("supplierTypeCode")
                    .expectedNode("RASSINI_ERP_Supplier_Type")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Tipo de proveedor nulo o vacío")
                    .build());
        }

        if (isBlank(supplier.getPurchaseTypeCode())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(businessUnit)
                    .outputType(OutputType.XML)
                    .subType("creditor")
                    .fieldName("purchaseTypeCode")
                    .expectedNode("RASSINI_ERP_Payment_Type")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Tipo de compra / pago nulo o vacío")
                    .build());
        }

        return valid;
    }

    public IssueSeverity evaluateIntegrityRow(SuppliersRowEntity supplier, String bu) {
        String supplierCode = supplier.getSupplierCode();
        String erpIdQad = supplier.getErpIdQad();
        String maskedAccount = maskAccount(supplier.getAccountNumber());

        // Bloqueantes bancarios
        if (isBlank(supplier.getAccountNumber())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(bu)
                    .maskedAccountNumber(maskedAccount)
                    .outputType(OutputType.INTEGRITY)
                    .subType("sync_file")
                    .fieldName("accountNumber")
                    .expectedNode("ERP_Bank_List[].Bank_Account_Number")
                    .issueType(IssueType.BANK_ACCOUNT_MISSING)
                    .severity(IssueSeverity.BLOCKING)
                    .result(OutputResult.NOT_GENERATED)
                    .technicalMessage("Número de cuenta bancaria nulo o vacío")
                    .build());
            return IssueSeverity.BLOCKING;
        }

        IssueSeverity worst = IssueSeverity.INFO;

        // Validar campos de posición requeridos por Integrity
        if (isBlank(supplier.getBeneficiaryBankName())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(bu)
                    .maskedAccountNumber(maskedAccount)
                    .outputType(OutputType.INTEGRITY)
                    .subType("sync_file")
                    .fieldName("beneficiaryBankName")
                    .expectedNode("ERP_Bank_List[].Bank_Name")
                    .issueType(IssueType.BANK_INFO_INCOMPLETE)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Nombre de banco beneficiario nulo o vacío")
                    .build());
            worst = IssueSeverity.WARNING;
        }

        if (isBlank(supplier.getSupplierCurrency())) {
            collector.recordIssue(MissingDataIssue.builder()
                    .supplierCode(supplierCode)
                    .erpIdQad(erpIdQad)
                    .businessUnitCode(bu)
                    .maskedAccountNumber(maskedAccount)
                    .outputType(OutputType.INTEGRITY)
                    .subType("sync_file")
                    .fieldName("supplierCurrency")
                    .expectedNode("ERP_Bank_List[].Bank_Currency_List")
                    .issueType(IssueType.REQUIRED_FIELD_BLANK)
                    .severity(IssueSeverity.WARNING)
                    .result(OutputResult.GENERATED_MANUAL)
                    .technicalMessage("Moneda bancaria nula o vacía")
                    .build());
            worst = IssueSeverity.WARNING;
        }

        return worst;
    }

    public void validateAlternateBankNodes(GraphiteSupplierDto dto) {
        if (dto == null || dto.getErpRecords() == null) return;

        for (GraphiteSupplierDto.ErpRecord erp : dto.getErpRecords()) {
            String bu = erp.getRassiniErpEntityId();
            List<GraphiteSupplierDto.Bank> banks = erp.getErpBankList();
            int bankCount = banks != null ? banks.size() : 0;

            if (erp.getBankWireAbaRouting() != null) {
                String altRouting = erp.getBankWireAbaRouting().getRouting();
                String altBankName = erp.getBankWireAbaRouting().getBankName();

                if (bankCount > 1) {
                    // Hay dato a nivel ERP_Record pero múltiples cuentas bancarias sin mapeo unívoco
                    collector.recordIssue(MissingDataIssue.builder()
                            .supplierCode(dto.getEntityPublicId())
                            .erpIdQad(dto.getErpIdQad())
                            .businessUnitCode(bu)
                            .outputType(OutputType.INTEGRITY)
                            .subType("sync_file")
                            .fieldName("Bank_Wire_ABA_Routing")
                            .expectedNode("ERP_Record[].ERP_Bank_List[].Bank_Wire_ABA_Routing")
                            .alternateNodeFound("ERP_Record[].Bank_Wire_ABA_Routing (routing=" + altRouting + ", bank=" + altBankName + ")")
                            .issueType(IssueType.AMBIGUOUS_NODE)
                            .severity(IssueSeverity.WARNING)
                            .result(OutputResult.GENERATED_MANUAL)
                            .technicalMessage("Dato bancario ABA/Routing encontrado a nivel ERP_Record pero existen múltiples cuentas (" + bankCount + "). Asociación ambigua.")
                            .build());
                } else if (bankCount == 1) {
                    log.info("[ALTERNATE-NODE-RESOLVED] supplier={} bu={} ABA routing resuelto unívocamente desde ERP_Record para cuenta única",
                            dto.getEntityPublicId(), bu);
                }
            }
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) return "N/A";
        if (account.length() <= 4) return "****";
        return "****" + account.substring(account.length() - 4);
    }
}
