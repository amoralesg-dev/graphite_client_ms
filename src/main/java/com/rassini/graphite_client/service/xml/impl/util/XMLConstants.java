package com.rassini.graphite_client.service.xml.impl.util;

public class XMLConstants {

    // Básicos
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String CREATE = "Create";
    public static final String CERO = "0";
    public static final String NULL = "NULL";
    public static final String LANG_CODE = "ls";
    public static final String PROVEEDOR = "PROVEEDOR";
    public static final String CONTACT_MALE = "MALE";

    // Versiones / Context
    public static final String CONTEXT_VERSION = "9.2";

    // Auditoría OC (FIJOS, NO fecha/hora del sistema)
    public static final String LAST_MODIFIED_USER = "mfg";
    public static final String LAST_MODIFIED_TIME = "46780";
    public static final String OC_LAST_MODIFIED_DATE = "2026-4-13";

    // RowIds OC (fijos por layout)
    public static final String ROW_ID = "0x000000000005d382";
    public static final String PARENT_ROW_ID = "0x000000000005dfc3";
    public static final String CONTACT_ROW_ID = "0x000000000005e6c1";

    // Address
    public static final String ADDRESS_LOGIC_KEY = "413826";
    public static final String CATALOG_STATE = "state";
    

    private XMLConstants() {
        // no instanciable
    }
}
