package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SupplierEntity;

public interface XmlPnService 
    extends XmlBusinessRelationService,
            XmlCreditorService
{
    void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter);
}
