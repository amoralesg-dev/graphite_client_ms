-- =====================================================================
-- seed_additional_test_suppliers_ver.sql
-- Inserción de versiones históricas adicionales (versión 2 y versión 3)
-- para los proveedores de prueba en `suppliers_ver`.
--
-- REGLAS ESTRICTAS:
-- 1. NO modifica la tabla `suppliers` (los datos vigentes permanecen intactos).
-- 2. Cada versión tiene un número incremental coherente (versión 2, versión 3).
-- 3. Las fechas (created_at) son cronológicas:
--      versión 1 (agosto/septiembre) -> versión 2 (octubre) -> versión 3 (noviembre) -> actual vigente
-- 4. `supplier_id` se toma dinámicamente de `suppliers.id`.
-- 5. `changed_fields` lista exactamente las columnas bancarias alteradas.
-- 6. version_source = 'TEST'.
-- =====================================================================

-- =====================================================================
-- PROVEEDOR 1: MX184608 (id = 1, BU = 0111, MXN)
-- Historial cronológico:
--   v1 (2026-08-15): BBVA (BCMRMXMM), CLABE ****1199
--   v2 (2026-09-25): Banco Santander (BMSXMXMM), CLABE ****2288 -> cambió banco, cuenta, swift
--   v3 (2026-10-20): Banorte (MENOMXMT), pero moneda era USD temporalmente -> cambió banco, swift, moneda
--   Vigente (actual): Banorte (MENOMXMT), MXN, CLABE ****4448
-- =====================================================================

-- Versión 2 de MX184608
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    2,
    'beneficiary_bank_name,account_number,routing_code_swift',
    'TEST',
    '2026-09-25 11:20:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    'Banco Santander Mexico SA',       -- v2 banco
    s.beneficiary_account_name,
    '014180004448222288',              -- v2 cuenta simulada (enmascarada: ****2288)
    s.bank_currency,                   -- MXN
    s.bank_country,                    -- MX
    s.routing_code_aba,
    'BMSXMXMM',                        -- v2 swift Santander
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX184608' AND s.id = 1;

-- Versión 3 de MX184608
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    3,
    'bank_currency',
    'TEST',
    '2026-10-20 16:45:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,           -- Banorte (mismo que vigente)
    s.beneficiary_account_name,
    s.account_number,                  -- CLABE actual ya vigente
    'USD',                             -- v3 tenía divisa USD (en vigente es MXN)
    s.bank_country,
    s.routing_code_aba,
    s.routing_code_swift,              -- MENOMXMT (mismo que vigente)
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX184608' AND s.id = 1;


-- =====================================================================
-- PROVEEDOR 2: US1205 (id = 3, BU = 0111, USD)
-- Historial cronológico:
--   v1 (2026-09-01): JPMorgan Chase, ABA 021000021, SWIFT CHASUS33
--   v2 (2026-09-28): Bank of America, ABA 026009593, SWIFT BOFAUS3N -> cambió banco, aba, swift
--   v3 (2026-10-25): Silicon Valley Bank, pero sin SWIFT -> cambió banco, swift
--   Vigente (actual): Silicon Valley Bank, ABA 121140399, SWIFT SVBKUS6S
-- =====================================================================

-- Versión 2 de US1205
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    2,
    'beneficiary_bank_name,routing_code_aba,routing_code_swift',
    'TEST',
    '2026-09-28 09:15:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    'Bank of America, N.A.',           -- v2 banco
    s.beneficiary_account_name,
    s.account_number,
    s.bank_currency,
    s.bank_country,
    '026009593',                       -- v2 ABA BofA
    'BOFAUS3N',                        -- v2 SWIFT BofA
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'US1205' AND s.id = 3;

-- Versión 3 de US1205
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    3,
    'routing_code_swift',
    'TEST',
    '2026-10-25 15:30:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,           -- Silicon Valley Bank
    s.beneficiary_account_name,
    s.account_number,
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,                -- 121140399
    NULL,                              -- v3 temporalmente no tenía SWIFT configurado
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'US1205' AND s.id = 3;


-- =====================================================================
-- PROVEEDOR 3: MX185296 (Cuenta 1: id = 12, BU = 09, MXN)
-- Historial cronológico:
--   v1 (2026-09-10): Banco Santander Mexico (BMSXMXMM)
--   v2 (2026-10-05): HSBC Mexico (HSBCMXMM) -> cambió banco, swift
--   v3 (2026-10-28): Citibanamex (BNMXMXMM), pero con otra CLABE -> cambió cuenta
--   Vigente (actual): Citibanamex (BNMXMXMM), CLABE ****5534
-- =====================================================================

-- Versión 2 de MX185296 (Cuenta 1)
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    2,
    'beneficiary_bank_name,routing_code_swift',
    'TEST',
    '2026-10-05 12:00:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    'HSBC Mexico SA',                  -- v2 banco
    s.beneficiary_account_name,
    s.account_number,
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    'HSBCMXMM',                        -- v2 SWIFT HSBC
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX185296' AND s.id = 12;

-- Versión 3 de MX185296 (Cuenta 1)
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    3,
    'account_number',
    'TEST',
    '2026-10-28 17:10:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,           -- Banco Nacional de Mexico SA
    s.beneficiary_account_name,
    '002180005534778899',              -- v3 CLABE anterior simulada (enmascarada: ****8899)
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    s.routing_code_swift,              -- BNMXMXMM
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX185296' AND s.id = 12;


-- =====================================================================
-- PROVEEDOR 3: MX185296 (Cuenta 2: id = 35767, BU = 09, USD)
-- Historial cronológico:
--   v1 (2026-09-12): Intermediario Citibank (CITIUS33), cuenta ****5678
--   v2 (2026-10-08): Intermediario JPMorgan (CHASUS33), cuenta ****9012 -> cambió intermediario
--   v3 (2026-10-30): Mismo banco y cuenta pero beneficiario era "GONCAR LOGISTICS INC" -> cambió beneficiario
--   Vigente (actual): Sin intermediario bancario (NULL)
-- =====================================================================

-- Versión 2 de MX185296 (Cuenta 2)
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    2,
    'intermediary_bank_name,intermediary_account,intermediary_account_country,intermediary_routing_code_swift',
    'TEST',
    '2026-10-08 14:00:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,
    s.beneficiary_account_name,
    s.account_number,
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    s.routing_code_swift,
    'JPMorgan Chase Bank, N.A.',       -- v2 intermediario JPMorgan
    '480099887766',                    -- v2 cuenta intermediario (enmascarada: ****7766)
    'US',
    s.intermediary_routing_code_aba,
    'CHASUS33',                        -- v2 SWIFT intermediario
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX185296' AND s.id = 35767;

-- Versión 3 de MX185296 (Cuenta 2)
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    3,
    'beneficiary_account_name',
    'TEST',
    '2026-10-30 10:15:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,
    'GONCAR LOGISTICS INC',            -- v3 nombre beneficiario previo (en vigente: GONCAR LOGISTICS SA DE CV)
    s.account_number,
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    s.routing_code_swift,
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX185296' AND s.id = 35767;


-- =====================================================================
-- PROVEEDOR 4: MX186126 (id = 15, BU = 0301, MXN)
-- Historial cronológico:
--   v1 (2026-09-20): CLABE ****8877, titular "ABM GRUPO ASESOR SC"
--   v2 (2026-10-10): Banregio (BRMEXMM), CLABE ****3344 -> cambió banco, cuenta, swift
--   v3 (2026-11-02): BBVA (BCMRMXMM), pero con código SWIFT genérico -> cambió swift
--   Vigente (actual): BBVA Mexico, SA (BCMRMXMM), CLABE ****9562
-- =====================================================================

-- Versión 2 de MX186126
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    2,
    'beneficiary_bank_name,account_number,routing_code_swift',
    'TEST',
    '2026-10-10 11:30:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    'Banco Regional de Monterrey SA',  -- v2 Banregio
    s.beneficiary_account_name,
    '058180009562334411',              -- v2 CLABE Banregio (enmascarada: ****4411)
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    'BRMEXMM',                         -- v2 SWIFT Banregio
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX186126' AND s.id = 15;

-- Versión 3 de MX186126
INSERT INTO suppliers_ver (
    supplier_id, version_number, changed_fields, version_source, created_at,
    erp_id_qad, business_unit_code, supplier_code, supplier_name, supplier_search_name, rfc,
    street_name, street_name2, street_name3, street_number, zip_code, city_code,
    state_code, state_description, country_code, contact_name, contact_email,
    supplier_code_dis_integrity, supplier_currency, purchase_type_code, supplier_type_code,
    beneficiary_bank_name, beneficiary_account_name, account_number, bank_currency, bank_country,
    routing_code_aba, routing_code_swift, intermediary_bank_name, intermediary_account,
    intermediary_account_country, intermediary_routing_code_aba, intermediary_routing_code_swift,
    status_integrity, xml_status
)
SELECT
    s.id,
    3,
    'routing_code_swift',
    'TEST',
    '2026-11-02 13:40:00.000000',
    s.erp_id_qad, s.business_unit_code, s.supplier_code, s.supplier_name, s.supplier_search_name, s.rfc,
    s.street_name, s.street_name2, s.street_name3, s.street_number, s.zip_code, s.city_code,
    s.state_code, s.state_description, s.country_code, s.contact_name, s.contact_email,
    s.supplier_code_dis_integrity, s.supplier_currency, s.purchase_type_code, s.supplier_type_code,
    s.beneficiary_bank_name,           -- BBVA Mexico, SA
    s.beneficiary_account_name,
    s.account_number,                  -- CLABE vigente
    s.bank_currency,
    s.bank_country,
    s.routing_code_aba,
    'BCMRMXMMXXX',                     -- v3 SWIFT con sucursal XXX (vigente: BCMRMXMM)
    s.intermediary_bank_name, s.intermediary_account, s.intermediary_account_country,
    s.intermediary_routing_code_aba, s.intermediary_routing_code_swift,
    s.status_integrity, s.xml_status
FROM suppliers s
WHERE s.supplier_code = 'MX186126' AND s.id = 15;
