package com.rassini.graphite_client.service.catalog;

public interface CatalogEquivalenciaFaltanteService {

     void registrar(
            String publicId,
            String idCatalogo,
            String code,
            String businessUnit);
}
