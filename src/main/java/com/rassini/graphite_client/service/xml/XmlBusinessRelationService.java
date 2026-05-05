package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;

public interface XmlBusinessRelationService {

    /**
     * Default para no forzar implementación inmediata en todas las plantas.
     * La implementación real se hará en cada ServiceImpl cuando se migre.
     */
    default void generateBusinessRelation(GraphiteSupplierDto dto) {
        // no-op
    }
}