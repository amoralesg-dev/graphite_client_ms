package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;

public interface XmlFrenosService 
    extends XmlBusinessRelationService,
            XmlCreditorService
 {
    void generate(GraphiteSupplierDto dto);
}
