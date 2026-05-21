package com.rassini.graphite_client.service.xml.impl;


import com.rassini.graphite_client.service.catalog.CatalogManagerCacheService;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogServiceImpl implements CatalogService {


    private final CatalogManagerCacheService catalogManagerCacheService;



    @Override
    public String mapCountry(String graphiteCountry, String plantId) {
        return graphiteCountry;
    }

    @Override
    public String mapCurrency(String graphiteCurrency, String plantId) {
        // ejemplo básico
        if ("MX".equalsIgnoreCase(graphiteCurrency) || "MEX".equalsIgnoreCase(graphiteCurrency)) {
            return "MXN";
        }
        return graphiteCurrency;
    }

    @Override
    public GlProfile resolveGlProfile(String plantId, String currency, boolean foreign) {
        // implementación mínima (placeholder)
        return new GlProfile(
                "P_20010001",
                "P_20010001",
                "P_20010001",
                "P_5001",
                "P_Compras"
        );
    }
    @Override
    public String getEquivalenciaState(String graphiteState, String plantId) {
        String equivalencia =null;
        equivalencia = catalogManagerCacheService.getEquivalencia(
                XMLConstants.CATALOG_STATE, graphiteState,plantId);

        if(equivalencia == null) {
            log.info("No se encontró equivalencia para state='{}' y plantId='{}'. Se debe enviar correo pero aun no implementado", graphiteState, plantId);
        }else {
            log.info("Equivalencia encontrada para state='{}' y plantId='{}': '{}'", graphiteState, plantId, equivalencia);
        }
        return equivalencia;
    }

    @Override
    public String resolveTaxClass(String plantId, String taxClass) {

        // Si Graphite trae el valor, se usa directo
        if (taxClass != null && !taxClass.isBlank()) {
            return taxClass;
        }

        // Para PN se permite default
        if ("09".equals(plantId)) {
            return "A17";
        }

        // OC / RFRENOS: NO inventar
        return null;
    }

    @Override
    public String resolvePaymentTerms(String plantId, String paymentTerms) {

        if (paymentTerms != null && !paymentTerms.isBlank()) {
            return paymentTerms;
        }

        // PN sí tiene default
        if ("09".equals(plantId)) {
            return "PN-04";
        }

        // OC / RFRENOS: pendiente por Graphite
        return null;
    }

    @Override
    public String resolvePurchaseType(String plantId, String paymentType) {
        return paymentType != null ? paymentType : "GVAR";
    }

    @Override
    public String resolveSupplierType(String plantId, String supplierType) {
        return supplierType != null ? supplierType : "NC";
    }
}
