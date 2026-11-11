-- =====================================================================
-- create_suppliers_ver.sql
-- Historial de versiones bancarias de proveedores (suppliers_ver).
-- =====================================================================

CREATE TABLE IF NOT EXISTS suppliers_ver (
    id                               BIGINT          NOT NULL AUTO_INCREMENT,
    supplier_id                      BIGINT          NOT NULL COMMENT 'Referencia a suppliers.id del registro versionado',
    version_number                   INT             NOT NULL COMMENT 'Numero incremental 1..N por cada proveedor/cuenta',
    changed_fields                   VARCHAR(1000)   NULL     COMMENT 'Lista CSV de columnas bancarias que cambiaron',
    version_source                   VARCHAR(30)     NOT NULL DEFAULT 'GRAPHITE',
    created_at                       DATETIME(6)     NOT NULL COMMENT 'Fecha y hora exacta de captura de la version previa',

    -- Fotografia completa de suppliers previa al cambio (mismos nombres, tipos, nulos y collation)
    erp_id_qad                       VARCHAR(50)     NULL,
    business_unit_code               VARCHAR(20)     NOT NULL,
    supplier_code                    VARCHAR(50)     NULL,
    supplier_name                    VARCHAR(255)    NULL,
    supplier_search_name             VARCHAR(50)     NULL,
    rfc                              VARCHAR(50)     NULL,
    street_name                      VARCHAR(255)    NULL,
    street_name2                     VARCHAR(255)    NULL,
    street_name3                     VARCHAR(255)    NULL,
    street_number                    VARCHAR(50)     NULL,
    zip_code                         VARCHAR(20)     NULL,
    city_code                        VARCHAR(100)    NULL,
    state_code                       VARCHAR(20)     NULL,
    state_description                VARCHAR(150)    NULL,
    country_code                     VARCHAR(20)     NULL,
    contact_name                     VARCHAR(150)    NULL,
    contact_email                    VARCHAR(150)    NULL,
    supplier_code_dis_integrity      VARCHAR(50)     NULL,
    supplier_currency                VARCHAR(10)     NULL,
    purchase_type_code               VARCHAR(50)     NULL,
    supplier_type_code               VARCHAR(50)     NULL,
    beneficiary_bank_name            VARCHAR(200)    NULL,
    beneficiary_account_name         VARCHAR(200)    NULL,
    account_number                   VARCHAR(50)     NULL,
    bank_currency                    VARCHAR(10)     NULL,
    bank_country                     VARCHAR(20)     NULL,
    routing_code_aba                 VARCHAR(50)     NULL,
    routing_code_swift               VARCHAR(50)     NULL,
    intermediary_bank_name           VARCHAR(200)    NULL,
    intermediary_account             VARCHAR(50)     NULL,
    intermediary_account_country     VARCHAR(20)     NULL,
    intermediary_routing_code_aba    VARCHAR(50)     NULL,
    intermediary_routing_code_swift  VARCHAR(50)     NULL,
    status_integrity                 CHAR(1)         NULL,
    xml_status                       VARCHAR(50)     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_ver_supplier_version (supplier_id, version_number),
    KEY idx_suppliers_ver_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
