package com.rassini.graphite_client.service.xml;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;

public interface XmlCreditorService {

    /**
     * Default para no forzar implementación inmediata en todas las plantas.
     * La implementación real se hará en cada ServiceImpl cuando se migre.
     */
    default void generateCreditor(GraphiteSupplierDto dto) {
        // no-op
    }
}