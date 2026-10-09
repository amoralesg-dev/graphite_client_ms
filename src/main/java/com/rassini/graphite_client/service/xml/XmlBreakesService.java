package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SupplierEntity;
public interface XmlBreakesService {
    void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter);

    default void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter, boolean overwriteIfExists) {
        generate(dto, supplierParameter);
    }
}