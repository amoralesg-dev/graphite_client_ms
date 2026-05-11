package com.rassini.graphite_client.service.xml.impl.util;

import java.util.Locale;
import java.util.Map;

/**
 * Reglas QAD basadas en el "archivo de César".
 * NO accede a BD. Solo resuelve códigos / perfiles según dominio (planta) + entradas.
 */
public final class CesarQadRules {

    private CesarQadRules() {}

    // =========================================================
    // Dominios / Plantas
    // =========================================================
    public enum Domain {
        RPIEDRAS,   // PN
        RBYPASA,    // BYPASA
        RFCORPO,    // CORPORATIVO
        RFRENOS,    // FRENOS
        RBRAKES     // BRAKES
    }

    // =========================================================
    // Resultado Perfiles GL
    // =========================================================
    public record GlProfiles(
            String invControl,
            String cnControl,
            String prepayControl,
            String divProfile,
            String purchaseGlProfile
    ) {}

    // =========================================================
    // 1) TIPOS DE PROVEEDOR
    // =========================================================

    private static final Map<String, String> SUPPLIER_TYPE_PN =
            Map.ofEntries(
                    Map.entry("EMPLEADOS Y OBREROS DE RASSINI", "EMPL"),
                    Map.entry("COMPONENTES IMPORTADOS", "IMCO"),
                    Map.entry("MATERIA PRIMA IMPORTADA", "IMMP"),
                    Map.entry("COMPONENTES NACIONALES", "NACO"),
                    Map.entry("FLETES NACIONALES", "NAFT"),
                    Map.entry("MATERIA PRIMA NACIONAL", "NAMP"),
                    Map.entry("NO CLASIFICADO", "NC")
            );

    private static final Map<String, String> SUPPLIER_TYPE_CORPO =
            Map.ofEntries(
                    Map.entry("RAW MATERIAL", "RAW MATERIAL"),
                    Map.entry("VARIABLE", "VARIABLE"),
                    Map.entry("FIXED", "FIXED"),
                    Map.entry("OTHER", "OTHER")
            );

    private static final Map<String, String> SUPPLIER_TYPE_FRENOS_BRAKES =
            Map.ofEntries(
                    Map.entry("DIRECTO", "DIRECTO"),
                    Map.entry("INDIRECTO", "INDIRECTO"),
                    Map.entry("REFACCIONES", "REFACCIONES"),
                    Map.entry("SERVICIO", "SERVICIO")
            );

    public static String resolveSupplierTypeCode(Domain domain, String supplierTypeText) {
        if (isBlank(supplierTypeText)) return null;
        String key = supplierTypeText.trim();

        return switch (domain) {
            case RPIEDRAS -> SUPPLIER_TYPE_PN.getOrDefault(key, key);
            case RFCORPO  -> SUPPLIER_TYPE_CORPO.getOrDefault(key, key);
            case RFRENOS, RBRAKES -> SUPPLIER_TYPE_FRENOS_BRAKES.getOrDefault(key, key);
            case RBYPASA  -> key;
        };
    }

    // =========================================================
    // 2) TÉRMINOS DE CRÉDITO
    // =========================================================

    private static final Map<String, String> TERMS_PN =
            Map.ofEntries(
                    Map.entry("CONTADO VS ENTREGA", "PN-01"),
                    Map.entry("CONTADO COMERCIAL 8 DIAS", "PN-02"),
                    Map.entry("15 DIAS P/FACTURA", "PN-03"),
                    Map.entry("30 DIAS P/FACTURA", "PN-04"),
                    Map.entry("45 DIAS P/FACTURA", "PN-05"),
                    Map.entry("60 DIAS P/FACTURA", "PN-06"),
                    Map.entry("50% ANT 50% C/ENTREGA", "PN-07"),
                    Map.entry("50% ANT 50% 8 DIAS P/F", "PN-08"),
                    Map.entry("50% ANT 50% 15 DIAS P/F", "PN-09"),
                    Map.entry("100% PAGO ANTICIPADO", "PN-10"),
                    Map.entry("VER NOTAS", "PN-11"),
                    Map.entry("90 DIAS FECHA FACTURA", "PN-12"),
                    Map.entry("SEGUN ACUERDO", "PN-13")
            );

    private static final Map<String, String> TERMS_FRENOS_BRAKES =
            Map.ofEntries(
                    Map.entry("0", "0"),
                    Map.entry("15", "15"),
                    Map.entry("30", "30"),
                    Map.entry("45", "45"),
                    Map.entry("60", "60"),
                    Map.entry("90", "90"),
                    Map.entry("120", "120")
            );

    public static String resolvePaymentTerms(Domain domain, String termsFromGraphite) {
        if (isBlank(termsFromGraphite)) return null;
        String key = termsFromGraphite.trim();

        return switch (domain) {
            case RPIEDRAS -> TERMS_PN.getOrDefault(key, key);
            case RFRENOS, RBRAKES -> TERMS_FRENOS_BRAKES.getOrDefault(key, key);
            default -> key;
        };
    }

    // =========================================================
    // 3) PURCHASE TYPE (passthrough)
    // =========================================================

    public static String resolvePurchaseTypeCode(Domain domain, String paymentTypeFromGraphite) {
        if (isBlank(paymentTypeFromGraphite)) return null;
        return paymentTypeFromGraphite.trim();
    }

    // =========================================================
    // 4) TAX CLASS (passthrough)
    // =========================================================

    public static String resolveTaxClass(Domain domain, String taxClassFromGraphite) {
        if (isBlank(taxClassFromGraphite)) return null;
        return taxClassFromGraphite.trim();
    }

    // =========================================================
    // 5) PERFILES GL (ARCHIVO DE CÉSAR)
    // =========================================================

    public static GlProfiles resolveGlProfiles(
            Domain domain,
            String currency,
            boolean isForeign,
            boolean pnUsdIsForeignPerson
    ) {

        String cur = currency == null ? "" : currency.toUpperCase(Locale.ROOT);

        // ---- PIEDRAS NEGRAS ----
        if (domain == Domain.RPIEDRAS) {
            return switch (cur) {
                case "CAD" -> new GlProfiles("P_20090001", "P_20090001", "P_20090001", "P_5006", "P_Compras");
                case "EUR" -> new GlProfiles("P_20070001", "P_20070001", "P_20070001", "P_5006", "P_Compras");
                case "GBP" -> new GlProfiles("PF_20030006", "PF_20030006", "PF_20030006", "P_5006", "P_Compras");
                case "JPY" -> new GlProfiles("P_20030001", "P_20030001", "P_20030001", "P_5006", "P_Compras");
                case "MXN" -> new GlProfiles("P_20010001", "P_20010001", "P_20010001", "P_5001", "P_Compras");
                case "USD" -> pnUsdIsForeignPerson
                        ? new GlProfiles("P_20020001", "P_20020001", "P_20020001", "P_5006", "P_Compras")
                        : new GlProfiles("P_20010006", "P_20010006", "P_20010006", "P_5003", "P_Compras");
                default -> new GlProfiles("P_20090001", "P_20090001", "P_20090001", "P_5006", "P_Compras");
            };
        }

        // ---- CORPORATIVO / BYPASA ----
        if (domain == Domain.RFCORPO || domain == Domain.RBYPASA) {
            return isForeign
                    ? new GlProfiles("P_2030", "P_2030", "P_2030", "Sub_0622", "P_Compras")
                    : new GlProfiles("P_2010", "P_2010", "P_2010", "Sub_0620", "P_Compras");
        }

        // ---- FRENOS ----
        if (domain == Domain.RFRENOS) {
            return isForeign
                    ? new GlProfiles("2220", "2220", "2220", "0000", "9012")
                    : new GlProfiles("2210", "2210", "2210", "0000", "9012");
        }

        // ---- BRAKES ----
        if (domain == Domain.RBRAKES) {
            return new GlProfiles("2220", "2220", "2220", "4451", "5346");
        }

        return new GlProfiles(null, null, null, null, null);
    }

    // =========================================================
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}