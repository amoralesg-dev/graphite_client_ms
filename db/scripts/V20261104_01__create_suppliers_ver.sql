-- =====================================================================
-- V20261104_01__create_suppliers_ver.sql
-- Historial de versiones bancarias de suppliers (graphite_client_ms).
--
-- El proyecto NO usa Flyway/Liquibase (spring.jpa.hibernate.ddl-auto=none),
-- por lo que este script se ejecuta manualmente SOLO en la BD local:
--
--   mysql -h localhost -P 3306 -u <usuario_local> -p pagos_db < db/scripts/V20261104_01__create_suppliers_ver.sql
--
-- Es idempotente (CREATE TABLE IF NOT EXISTS). No modifica la tabla suppliers.
-- =====================================================================

CREATE TABLE IF NOT EXISTS suppliers_ver (
    id                               BIGINT       NOT NULL AUTO_INCREMENT,
    supplier_id                      BIGINT       NOT NULL COMMENT 'suppliers.id del registro versionado',
    version_number                   INT          NOT NULL COMMENT '1..n por supplier_id',
    changed_fields                   VARCHAR(1000)    NULL COMMENT 'Columnas bancarias que cambiaron (CSV)',
    version_source                   VARCHAR(30)  NOT NULL DEFAULT 'GRAPHITE',
    created_at                       DATETIME(6)  NOT NULL,

    -- Copia completa de suppliers (estado anterior al cambio bancario); tipos idénticos a suppliers
    erp_id_qad                       VARCHAR(50)     NULL,
    business_unit_code               VARCHAR(20) NOT NULL,
    supplier_code                    VARCHAR(50)     NULL,
    supplier_name                    VARCHAR(255)     NULL,
    supplier_search_name             VARCHAR(50)     NULL,
    rfc                              VARCHAR(50)     NULL,
    street_name                      VARCHAR(255)     NULL,
    street_name2                     VARCHAR(255)     NULL,
    street_name3                     VARCHAR(255)     NULL,
    street_number                    VARCHAR(50)     NULL,
    zip_code                         VARCHAR(20)     NULL,
    city_code                        VARCHAR(100)     NULL,
    state_code                       VARCHAR(20)     NULL,
    state_description                VARCHAR(150)     NULL,
    country_code                     VARCHAR(20)     NULL,
    contact_name                     VARCHAR(150)     NULL,
    contact_email                    VARCHAR(150)     NULL,
    supplier_code_dis_integrity      VARCHAR(50)     NULL,
    supplier_currency                VARCHAR(10)     NULL,
    purchase_type_code               VARCHAR(50)     NULL,
    supplier_type_code               VARCHAR(50)     NULL,
    beneficiary_bank_name            VARCHAR(200)     NULL,
    beneficiary_account_name         VARCHAR(200)     NULL,
    account_number                   VARCHAR(50)     NULL,
    bank_currency                    VARCHAR(10)     NULL,
    bank_country                     VARCHAR(20)     NULL,
    routing_code_aba                 VARCHAR(50)     NULL,
    routing_code_swift               VARCHAR(50)     NULL,
    intermediary_bank_name           VARCHAR(200)     NULL,
    intermediary_account             VARCHAR(50)     NULL,
    intermediary_account_country     VARCHAR(20)     NULL,
    intermediary_routing_code_aba    VARCHAR(50)     NULL,
    intermediary_routing_code_swift  VARCHAR(50)     NULL,
    status_integrity                 CHAR(1)     NULL,
    xml_status                       VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_ver_supplier_version (supplier_id, version_number),
    KEY idx_suppliers_ver_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
