package com.rassini.graphite_client.service.version;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.SuppliersVerEntity;

/**
 * Compara la información bancaria de un registro de {@code suppliers}.
 * <p>
 * Reglas: null-safe, {@code null} y cadena vacía/blanca se consideran iguales,
 * se ignoran espacios al inicio/fin. No aplica otras normalizaciones
 * (mayúsculas, quitar espacios internos) para no alterar las reglas de mapeo existentes.
 */
public final class SupplierBankDataComparator {

    /** Columnas de suppliers consideradas información bancaria. */
    public static final List<String> BANK_FIELDS = List.of(
            "beneficiary_bank_name",
            "beneficiary_account_name",
            "account_number",
            "bank_currency",
            "bank_country",
            "routing_code_aba",
            "routing_code_swift",
            "intermediary_bank_name",
            "intermediary_account",
            "intermediary_account_country",
            "intermediary_routing_code_aba",
            "intermediary_routing_code_swift");

    /** Campos que no deben aparecer completos en logs. */
    public static final Set<String> SENSITIVE_FIELDS = Set.of(
            "account_number",
            "intermediary_account",
            "routing_code_aba",
            "intermediary_routing_code_aba");

    private SupplierBankDataComparator() {
    }

    public static Map<String, String> bankData(SuppliersRowEntity s) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("beneficiary_bank_name", s.getBeneficiaryBankName());
        m.put("beneficiary_account_name", s.getBeneficiaryAccountName());
        m.put("account_number", s.getAccountNumber());
        m.put("bank_currency", s.getBankCurrency());
        m.put("bank_country", s.getBankCountry());
        m.put("routing_code_aba", s.getRoutingCodeAba());
        m.put("routing_code_swift", s.getRoutingCodeSwift());
        m.put("intermediary_bank_name", s.getIntermediaryBankName());
        m.put("intermediary_account", s.getIntermediaryAccount());
        m.put("intermediary_account_country", s.getIntermediaryAccountCountry());
        m.put("intermediary_routing_code_aba", s.getIntermediaryRoutingCodeAba());
        m.put("intermediary_routing_code_swift", s.getIntermediaryRoutingCodeSwift());
        return m;
    }

    public static Map<String, String> bankData(SuppliersVerEntity s) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("beneficiary_bank_name", s.getBeneficiaryBankName());
        m.put("beneficiary_account_name", s.getBeneficiaryAccountName());
        m.put("account_number", s.getAccountNumber());
        m.put("bank_currency", s.getBankCurrency());
        m.put("bank_country", s.getBankCountry());
        m.put("routing_code_aba", s.getRoutingCodeAba());
        m.put("routing_code_swift", s.getRoutingCodeSwift());
        m.put("intermediary_bank_name", s.getIntermediaryBankName());
        m.put("intermediary_account", s.getIntermediaryAccount());
        m.put("intermediary_account_country", s.getIntermediaryAccountCountry());
        m.put("intermediary_routing_code_aba", s.getIntermediaryRoutingCodeAba());
        m.put("intermediary_routing_code_swift", s.getIntermediaryRoutingCodeSwift());
        return m;
    }

    /** Devuelve las columnas bancarias que cambiaron entre la versión anterior y la nueva. */
    public static List<String> changedFields(SuppliersVerEntity previous, SuppliersRowEntity updated) {
        Map<String, String> before = bankData(previous);
        Map<String, String> after = bankData(updated);
        List<String> changed = new ArrayList<>();
        for (String field : BANK_FIELDS) {
            if (!normalize(before.get(field)).equals(normalize(after.get(field)))) {
                changed.add(field);
            }
        }
        return changed;
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /** Enmascara conservando los últimos 4 caracteres. */
    public static String mask(String value) {
        String v = normalize(value);
        if (v.isEmpty()) return "N/A";
        if (v.length() <= 4) return "****";
        return "*".repeat(v.length() - 4) + v.substring(v.length() - 4);
    }

    /** Representación segura para logs. */
    public static String safeForLog(String field, String value) {
        return SENSITIVE_FIELDS.contains(field) ? mask(value) : (value == null ? "null" : value.trim());
    }
}
