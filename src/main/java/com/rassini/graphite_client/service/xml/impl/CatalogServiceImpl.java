package com.rassini.graphite_client.service.xml.impl;


import com.rassini.graphite_client.dto.UpdateInfo;
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
    public String mapCountry(String publicId, String graphiteCountry, String plantId) {
        String equivalencia =null;
        equivalencia = catalogManagerCacheService.getEquivalencia(
                XMLConstants.CATALOG_COUNTRY, graphiteCountry,plantId);

        if(equivalencia == null) {
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_COUNTRY,
                    graphiteCountry,
                    plantId,
                    "graphite"
            );
            log.info("No se encontró equivalencia para country='{}' y plantId='{}'. Se debe enviar correo pero aun no implementado", graphiteCountry, plantId);
        }else {
            log.info("Equivalencia encontrada para country='{}' y plantId='{}': '{}'", graphiteCountry, plantId, equivalencia);
        }
        return equivalencia;
    }
    @Override
    public String mapCountry09(String publicId, String graphiteCountry, String plantId) {
        String equivalencia =null;
        equivalencia = catalogManagerCacheService.getEquivalencia(
                XMLConstants.CATALOG_COUNTRY_INTEGITY, graphiteCountry,plantId);

        if(equivalencia == null) {
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_COUNTRY_INTEGITY,
                    graphiteCountry,
                    plantId,
                    "Integrity"
            );
            log.info("No se encontró equivalencia para country='{}' y plantId='{}'. Se debe enviar correo proceso{}", graphiteCountry, plantId,"Integrity");
        }else {
            log.info("Equivalencia encontrada para country='{}' y plantId='{}': '{}'", graphiteCountry, plantId, equivalencia);
        }
        return equivalencia;
    }
    @Override
    public UpdateInfo resolveUpdateInfo(SuppliersRowEntity supplier) {

        String businessUnit = supplier.getBusinessUnitCode();
        String statusIntegrity = supplier.getStatusIntegrity();

        String partialUpdate = XMLConstants.FALSE;
        String activityCode = XMLConstants.CREATE;

        log.info(
                "Evaluando update info supplierCode={}, businessUnit={}, statusIntegrity={}",
                supplier.getSupplierCode(),
                businessUnit,
                statusIntegrity);

        // Alta o registro nuevo
        if (statusIntegrity == null
                || XMLConstants.ALTA.equalsIgnoreCase(statusIntegrity)) {

            activityCode = XMLConstants.CREATE;
            partialUpdate = XMLConstants.FALSE;
        }

        // Modificación
        else if (XMLConstants.MOD.equalsIgnoreCase(statusIntegrity)) {

            partialUpdate = XMLConstants.TRUE;

            // Corpo
            if (XMLConstants.OC.equals(businessUnit)) {
                activityCode = XMLConstants.MODIFY; // Activity Code
            }

            // Frenos
            else if (XMLConstants.FRENOS.equals(businessUnit)
                    || XMLConstants.BREAKES.equals(businessUnit)) {

                activityCode = XMLConstants.MODIFY;
            }

            // Suspensiones
            else if (XMLConstants.PN.equals(businessUnit)) {

                activityCode = XMLConstants.MODIFY;
            }
        }

        log.info(
                "Resuelto supplierCode={} partialUpdate={} activityCode={}",
                supplier.getSupplierCode(),
                partialUpdate,
                activityCode);

        return UpdateInfo.builder()
                .partialUpdate(partialUpdate)
                .activityCode(activityCode)
                .build();
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

        if (XMLConstants.FRENOS.equalsIgnoreCase(plantId) && ("MX".equalsIgnoreCase(graphiteCurrency) || "MEX".equalsIgnoreCase(graphiteCurrency))) {
            return "MN";
        }else if (XMLConstants.FRENOS.equalsIgnoreCase(plantId) && "USD".equalsIgnoreCase(graphiteCurrency)) {
            return "US";   
        }else{
            return graphiteCurrency;
        }
        
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
    public String getEquivalenciaState(String publicId, String graphiteState, String plantId) {
        String equivalencia =null;
        equivalencia = catalogManagerCacheService.getEquivalencia(
                XMLConstants.CATALOG_STATE, graphiteState,plantId);

        if(equivalencia == null) {
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_STATE,
                    graphiteState,
                    plantId,
                    "graphite"
            );
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
    
}
