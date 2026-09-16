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

  

    private String statusIntegrity(SuppliersRowEntity row, GraphiteSupplierDto dto) {

        String statusFromRow = (row.getId() == null) ? XMLConstants.ALTA : XMLConstants.MOD;
        String statusFromDto = null;

        

        String statusErpGraphite = dto.getStatusERPGraphite();


        if (statusErpGraphite != null && !statusErpGraphite.isEmpty()) {
            if (row.getId() != null && statusErpGraphite.equals(row.getErpIdQad())){
                statusFromDto = XMLConstants.MOD;
            }else{
                statusFromDto = XMLConstants.ALTA;
            }
        }

        
        String status = statusFromDto != null ? statusFromDto : statusFromRow;
        log.info("Estatus  {} resuelto para supplier{}",status, row.getSupplierCode());

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

                SuppliersRowEntity row = suppliersRowRepository
                .findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(
                        creditor,
                        bu,
                        accountRaw
                )
                .orElseGet(SuppliersRowEntity::new);

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


                //  llenar el MISMO objeto (no crear otro)

                SupplierRowMapper.fill(row, dto, hq, erp, bank, catalogService);

                if (dto.getStatusERPGraphite() != null && !dto.getStatusERPGraphite().isEmpty()){
                    row.setErpIdQad(dto.getStatusERPGraphite());
                }

                row.setSupplierCodeDisIntegrity(
                        resolveSupplierCodeDisIntegrity(
                                creditor,
                                row
                        )
                );

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

                //  guardar: si row ya tenía id -> UPDATE; si no -> INSERT
                SuppliersRowEntity savedRow = suppliersRowRepository.save(row);
                log.info("[FLOW-PHASE-2][PERSIST-ROW] supplier={} businessUnit={} account={} id={} xmlStatus={}",
                        creditor, bu, maskedAccount, savedRow.getId(), savedRow.getXmlStatus());
            }
        }
    }


    private String resolveSupplierCodeDisIntegrity(
        String creditor,
        SuppliersRowEntity row) {

        Optional<SuppliersRowEntity> existingAccount =
                suppliersRowRepository
                        .findFirstBySupplierCodeAndAccountNumber(
                                creditor,
                                row.getAccountNumber());

        if (existingAccount.isPresent()) {

            return existingAccount.get()
                    .getSupplierCodeDisIntegrity();
        }

        long distinctAccounts =
                suppliersRowRepository
                        .countDistinctAccountsBySupplierCode(
                                creditor);

        if (distinctAccounts == 0) {
            return row.getErpIdQad();
        }

        return row.getErpIdQad() + "_" + distinctAccounts;
    }

    private String maskAccountNumber(String account) {
        if (account == null || account.isBlank()) return "N/A";
        if (account.length() <= 4) return "****";
        return "****" + account.substring(account.length() - 4);
    }
}
