package com.rassini.graphite_client.service.xml.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.address.ResolvedAddress;
import com.rassini.graphite_client.service.address.SupplierAddressResolver;
import com.rassini.graphite_client.service.mapper.SupplierRowMapper;
import com.rassini.graphite_client.service.resolver.SupplierErpResolver;
import com.rassini.graphite_client.service.resolver.ErpResolutionResult;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.SupplierJpaMapper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierJpaMapperImpl implements SupplierJpaMapper {

    private final SuppliersRowRepository suppliersRowRepository;
    private final CatalogService catalogService;
    private final SupplierErpResolver supplierErpResolver;

  

    private String statusIntegrity(SuppliersRowEntity row, GraphiteSupplierDto dto) {

        String status = (row.getId() == null)
                ? XMLConstants.ALTA
                : XMLConstants.MOD;

        log.info(
            "[STATUS-INTEGRITY] supplier={} rowId={} statusIntegrity={}",
            dto.getEntityPublicId(),
            row.getId(),
            status
        );

        return status;
    }


    @Override
    public void upsertSuppliersRows(GraphiteSupplierDto dto) {

        if (dto == null || dto.getErpRecords() == null) return;

        GraphiteSupplierDto.Location hq = SupplierRowMapper.findHeadquarters(dto);

        for (GraphiteSupplierDto.ErpRecord erp : dto.getErpRecords()) {

            if (erp == null || erp.getRassiniErpEntityId() == null) continue;

            String creditor = dto.getEntityPublicId();
            String bu = erp.getRassiniErpEntityId();

            if (erp.getErpBankList() != null) {
                log.info(
                        "[FLOW-PHASE-2][BANK-PARSE] supplier={} businessUnit={} Cuentas bancarias encontradas={}",
                        creditor,
                        bu,
                        erp.getErpBankList().size()
                );
            }

            if (erp.getErpBankList() == null || erp.getErpBankList().isEmpty()) {
                log.warn("[FLOW-PHASE-2][BANK-PARSE] supplier={} businessUnit={} Sin cuentas bancarias en DTO", creditor, bu);
                continue;
            }

            for (GraphiteSupplierDto.Bank bank : erp.getErpBankList()) {

                String accountRaw = bank.getBankAccountNumber();
                String maskedAccount = maskAccountNumber(accountRaw);
                String legacyMappedErpId = dto.getLegacyMappedErpId();

                log.info("[LEGACY-LOOKUP] graphiteSupplierId={} legacySupplierCode={} businessUnit={} accountNumber={}",
                        creditor, legacyMappedErpId, bu, maskedAccount);

                Optional<SuppliersRowEntity> existingRowOpt = suppliersRowRepository
                        .findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(
                                creditor,
                                bu,
                                accountRaw
                        );

                // Fallback exacto para proveedores legacy: buscar por erp_id_qad histórico exacto (con BU y cuenta bancaria)
                if (existingRowOpt.isEmpty()
                        && dto.isLegacy()
                        && legacyMappedErpId != null
                        && !legacyMappedErpId.isBlank()) {
                    existingRowOpt = suppliersRowRepository
                            .findByErpIdQadAndBusinessUnitCodeAndAccountNumber(
                                    legacyMappedErpId,
                                    bu,
                                    accountRaw
                            );
                }

                log.info("[LEGACY-LOOKUP-RESULT] found={} rowId={} supplierCode={} erpIdQad={} businessUnit={} accountNumber={}",
                        existingRowOpt.isPresent(),
                        existingRowOpt.map(SuppliersRowEntity::getId).orElse(null),
                        existingRowOpt.map(SuppliersRowEntity::getSupplierCode).orElse(null),
                        existingRowOpt.map(SuppliersRowEntity::getErpIdQad).orElse(null),
                        bu,
                        maskedAccount);

                SuppliersRowEntity row = existingRowOpt.orElseGet(SuppliersRowEntity::new);

                log.info(
                    "[FLOW-PHASE-2][UPSERT-ROW] supplier={} businessUnit={} account={} id={} currentCode={}",
                    creditor,
                    bu,
                    maskedAccount,
                    row.getId(),
                    row.getSupplierCodeDisIntegrity()
                );

                String statusIntegrity = statusIntegrity(row, dto);

                log.info("[FLOW-PHASE-2][INTEGRITY-STATUS] supplier={} businessUnit={} statusIntegrity={}", creditor, bu, statusIntegrity);
                row.setStatusIntegrity(statusIntegrity);   


                // Capturar el erpIdQad, supplierCodeDisIntegrity e id de la fila persistida ANTES de fill
                String persistedErpIdQad = (row.getId() != null && row.getErpIdQad() != null && !row.getErpIdQad().isBlank())
                        ? row.getErpIdQad()
                        : null;
                String persistedDisIntegrity = (row.getId() != null && row.getSupplierCodeDisIntegrity() != null && !row.getSupplierCodeDisIntegrity().isBlank())
                        ? row.getSupplierCodeDisIntegrity()
                        : null;
                Long rowId = row.getId();

                // llenar el MISMO objeto (no crear otro)
                SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

                log.info("[ERP-PERSISTED-SOURCE] supplier={} businessUnit={} accountMasked={} rowId={} persistedErpIdQad={}",
                        creditor, bu, maskedAccount, rowId != null ? rowId : "NEW", persistedErpIdQad != null ? persistedErpIdQad : "null");

                // Resolver el ERP efectivo usando el componente compartido obligatorio
                ErpResolutionResult resolution = supplierErpResolver.resolveEffectiveErpId(
                        creditor,
                        legacyMappedErpId,
                        persistedErpIdQad,
                        dto.getErpIdQad(),
                        "SUPPLIER_CODE_DIS_INTEGRITY"
                );
                String effectiveErpId = resolution.getResolvedErpId();

                // Regla Oficial:
                // CASO 1: Supplier_Is_Legacy = y -> erp_id_qad = RASSINI_Legacy_QAD_ID (legacyMappedErpId)
                // CASO 2: Supplier_Is_Legacy = n -> erp_id_qad = RASSINI_ERP_ID (dto.getErpIdQad() o resuelto)
                if (dto.isLegacy() && legacyMappedErpId != null && !legacyMappedErpId.isBlank()) {
                    row.setErpIdQad(legacyMappedErpId);
                } else {
                    row.setErpIdQad(effectiveErpId);
                }

                // Base para supplier_code_dis_integrity:
                // Supplier_Is_Legacy = y -> base = RASSINI_Legacy_QAD_ID
                // Supplier_Is_Legacy = n -> base = RASSINI_ERP_ID (effectiveErpId)
                String baseDisIntegrity = (dto.isLegacy() && legacyMappedErpId != null && !legacyMappedErpId.isBlank())
                        ? legacyMappedErpId
                        : effectiveErpId;

                // Si la cuenta bancaria ya existía en la entidad recuperada, conservar su supplierCodeDisIntegrity histórico
                if (persistedDisIntegrity != null) {
                    row.setSupplierCodeDisIntegrity(persistedDisIntegrity);
                } else {
                    row.setSupplierCodeDisIntegrity(
                            resolveSupplierCodeDisIntegrity(
                                    creditor,
                                    legacyMappedErpId,
                                    row,
                                    baseDisIntegrity,
                                    dto.isLegacy()
                            )
                    );
                }

                // Validar si faltó alguna equivalencia de catálogo requerida (ej. estado)
                ResolvedAddress address = SupplierAddressResolver.resolve(dto, hq, erp);
                if (address != null && address.getRegion() != null && !address.getRegion().isBlank()
                        && (row.getStateCode() == null || row.getStateCode().isBlank())) {
                    row.setXmlStatus(XmlStatus.ERROR);
                    log.warn("[FLOW-PHASE-2][CATALOG-CHECK] supplier={} businessUnit={} catalogStatus=ERROR catalog=state code={}",
                            creditor, bu, address.getRegion());
                } else if (row.getXmlStatus() == null || XmlStatus.ERROR.equals(row.getXmlStatus())) {
                    // Si antes tenía error y ahora el estado resolvió correctamente, restaurar a PENDING
                    row.setXmlStatus(XmlStatus.PENDING);
                }

                // guardar: si row ya tenía id -> UPDATE; si no -> INSERT
                String operation = (row.getId() != null) ? "UPDATE" : "INSERT";
                log.info("[JPA-SAVE] rowId={} supplierCode={} businessUnit={} accountNumber={} operation={}",
                        row.getId(), row.getSupplierCode(), bu, maskedAccount, operation);

                SuppliersRowEntity savedRow = suppliersRowRepository.save(row);
                log.info("[FLOW-PHASE-2][PERSIST-ROW] supplier={} businessUnit={} account={} id={} xmlStatus={}",
                        creditor, bu, maskedAccount, savedRow.getId(), savedRow.getXmlStatus());
            }
        }
    }


    private String resolveSupplierCodeDisIntegrity(
        String creditor,
        String legacyMappedErpId,
        SuppliersRowEntity row,
        String baseDisIntegrity,
        boolean isLegacy) {

        Optional<SuppliersRowEntity> existingAccount =
                suppliersRowRepository
                        .findFirstBySupplierCodeAndAccountNumber(
                                creditor,
                                row.getAccountNumber());

        if (existingAccount.isEmpty()
                && legacyMappedErpId != null
                && !legacyMappedErpId.isBlank()
                && !legacyMappedErpId.equals(creditor)) {
            existingAccount = suppliersRowRepository
                    .findFirstBySupplierCodeAndAccountNumber(
                            legacyMappedErpId,
                            row.getAccountNumber());
        }

        // Búsqueda histórica por erp_id_qad en la cuenta bancaria (proveedores legacy)
        if (existingAccount.isEmpty()
                && legacyMappedErpId != null
                && !legacyMappedErpId.isBlank()) {
            existingAccount = suppliersRowRepository
                    .findFirstByErpIdQadAndAccountNumber(
                            legacyMappedErpId,
                            row.getAccountNumber());
        }

        if (existingAccount.isPresent()) {
            String existingCode = existingAccount.get().getSupplierCodeDisIntegrity();
            log.info(
                "[SUPPLIER-CODE-DIS-INTEGRITY] supplier={} account={} isLegacy={} existingCodeDisIntegrity={}",
                creditor,
                maskAccountNumber(row.getAccountNumber()),
                isLegacy,
                existingCode
            );
            return existingCode;
        }

        long distinctAccounts;
        if (isLegacy) {
            String lookupErpId = (legacyMappedErpId != null && !legacyMappedErpId.isBlank())
                    ? legacyMappedErpId
                    : baseDisIntegrity;
            distinctAccounts = suppliersRowRepository.countDistinctAccountsByErpIdQad(lookupErpId);
            if (distinctAccounts == 0 && !lookupErpId.equals(creditor)) {
                distinctAccounts = suppliersRowRepository.countDistinctAccountsBySupplierCode(creditor);
            }
        } else {
            distinctAccounts = suppliersRowRepository.countDistinctAccountsBySupplierCode(creditor);
            if (distinctAccounts == 0
                    && legacyMappedErpId != null
                    && !legacyMappedErpId.isBlank()
                    && !legacyMappedErpId.equals(creditor)) {
                distinctAccounts = suppliersRowRepository.countDistinctAccountsBySupplierCode(legacyMappedErpId);
            }
        }

        String resolvedCode;
        // Si es legacy y ya existen cuentas históricas (por ejemplo COCHGMER o 60000736),
        // la nueva cuenta incremental se numera a partir del total de cuentas existentes + 1
        if (isLegacy) {
            long nextIndex = distinctAccounts + 1;
            resolvedCode = baseDisIntegrity + "_" + nextIndex;
        } else if (distinctAccounts == 0) {
            resolvedCode = baseDisIntegrity;
        } else {
            resolvedCode = baseDisIntegrity + "_" + distinctAccounts;
        }

        log.info(
            "[SUPPLIER-CODE-DIS-INTEGRITY] supplier={} account={} isLegacy={} baseDisIntegrity={} distinctAccounts={} resolvedCode={}",
            creditor,
            maskAccountNumber(row.getAccountNumber()),
            isLegacy,
            baseDisIntegrity,
            distinctAccounts,
            resolvedCode
        );

        return resolvedCode;
    }

    private String maskAccountNumber(String account) {
        if (account == null || account.isBlank()) return "N/A";
        if (account.length() <= 4) return "****";
        return "****" + account.substring(account.length() - 4);
    }
}
