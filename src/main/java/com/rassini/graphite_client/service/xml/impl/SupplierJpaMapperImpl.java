package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.mapper.SupplierRowMapper;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.SupplierJpaMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierJpaMapperImpl implements SupplierJpaMapper {

    private final SuppliersRowRepository suppliersRowRepository;
    private final CatalogService catalogService;

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
                    .findByCreditorCodeAndBusinessUnitCode(creditor, bu)
                    .orElseGet(SuppliersRowEntity::new);

            
            if (row.getId() == null) {
                row.setStatus("A"); // Alta
            } else {
                row.setStatus("M"); // Modificación
            }


            //  llenar el MISMO objeto (no crear otro)

            row.getStatus();
            SupplierRowMapper.fill(row, dto, hq, erp, catalogService);

            
            if (row.getId() == null) {

                int existing =
                        suppliersRowRepository.countBySupplierCode(creditor);

                row.setSupplierCodeDisIntegrity(
                        creditor + (existing + 1)
                );
            }


            //  guardar: si row ya tenía id -> UPDATE; si no -> INSERT
            suppliersRowRepository.save(row);
        }
    }
}
