package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
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

        if (statusErpGraphite != null) {
            if ("CAMBIARDESPUESALTA".equalsIgnoreCase(statusErpGraphite)) {
                statusFromDto = XMLConstants.ALTA;
            } else if ("CAMBIARDESPUESMOD".equalsIgnoreCase(statusErpGraphite)) {
                statusFromDto = XMLConstants.MOD;
            } else if ("CAMBIARDESPUESBAJA".equalsIgnoreCase(statusErpGraphite)) {
                statusFromDto = XMLConstants.BAJA;
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

            //  UPSERT: si existe -> UPDATE; si no -> INSERT
            SuppliersRowEntity row = suppliersRowRepository
                    .findBySupplierCodeAndBusinessUnitCode(creditor, bu)
                    .orElseGet(SuppliersRowEntity::new);

            String statusIntegrity = statusIntegrity(row, dto);

            log.info("Status integrity resuelto: {} para supplierCode={} y businessUnitCode={}", statusIntegrity, creditor, bu);
            row.setStatusIntegrity(statusIntegrity);   


            //  llenar el MISMO objeto (no crear otro)

            SupplierRowMapper.fill(row, dto, hq, erp, catalogService);


            if (row.getId() == null) {

                int existing =
                        suppliersRowRepository.countBySupplierCodeAndAccountNumber(creditor, row.getAccountNumber());

                row.setSupplierCodeDisIntegrity(
                        row.getErpIdQad() + (existing + 1)
                );
            }


            //  guardar: si row ya tenía id -> UPDATE; si no -> INSERT
            suppliersRowRepository.save(row);
        }
    }
}
