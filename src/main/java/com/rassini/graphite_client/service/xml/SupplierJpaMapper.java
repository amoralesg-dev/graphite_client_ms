package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;

public interface SupplierJpaMapper {
    void upsertSuppliersRows(GraphiteSupplierDto dto);
}
