package com.rassini.graphite_client.service.xml.impl;


import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.service.catalog.CatalogEquivalenciaFaltanteService;
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

    private static final String DEFAULT_GL_PROFILE = "P_20010001";

    private final CatalogManagerCacheService catalogManagerCacheService;
    private final CatalogEquivalenciaFaltanteService catalogEquivalenciaFaltanteService;


    @Override
    public String mapCountry(String graphiteCountry, String plantId) {
        return graphiteCountry;
    }
    @Override
    public String getActivityCode(SuppliersRowEntity supplier) {
        String activityCode = null;

        log.info("Evaluando activity code para supplierCode={} con statusIntegrity={}", supplier.getSupplierCode(), supplier.getStatusIntegrity());

       if(supplier.getStatusIntegrity()==null ||XMLConstants.ALTA.equalsIgnoreCase(supplier.getStatusIntegrity())){
                activityCode=XMLConstants.CREATE;
        }
        log.info("Resuelto Activity code  para supplierCode={}: '{}'", supplier.getSupplierCode(), activityCode);
        return activityCode;
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
                DEFAULT_GL_PROFILE,
                DEFAULT_GL_PROFILE,
                DEFAULT_GL_PROFILE,
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
           /* catalogEquivalenciaFaltanteService.registrar(
                    XMLConstants.CATALOG_STATE,
                    graphiteState,
                    plantId
            );*/
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
    @Override
    public String getAction(SuppliersRowEntity supplier) {
        String action = null;

        log.info("Evaluando acción para supplierCode={} con statusIntegrity={}", supplier.getSupplierCode(), supplier.getStatusIntegrity());
        
        if(supplier.getStatusIntegrity() == null || XMLConstants.ALTA.equalsIgnoreCase(supplier.getStatusIntegrity())) {
           action = XMLConstants.SAVE;
        }
        log.info("Resuelto Action  para supplierCode={}: '{}'", supplier.getSupplierCode(), action);
        return action;
    }
    @Override
    public String getPartialUpdate(SuppliersRowEntity supplier) {
        String partialUpdate = null;

        log.info("Evaluando partial update para supplierCode={} con statusIntegrity={}", supplier.getSupplierCode(), supplier.getStatusIntegrity());
        
        if(XMLConstants.MOD.equalsIgnoreCase(supplier.getStatusIntegrity())) {
           partialUpdate = XMLConstants.TRUE;
        }
        log.info("Resuelto Partial update para supplierCode={}: '{}'", supplier.getSupplierCode(), partialUpdate);
        return partialUpdate;
    }
}
