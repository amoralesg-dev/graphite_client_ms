package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;

public interface XmlOcService
        extends XmlBusinessRelationService,
                XmlCreditorService {

    /**
     * Orquestador: por cada planta OC (0111/0301) debe generar:
     *  - 1 XML BusinessRelation (busrel)
     *  - 1 XML Creditor (creditor)
     */
    void generate(GraphiteSupplierDto dto);
}