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
    private final com.rassini.graphite_client.repository.SuppliersRowRepository suppliersRowRepository;


    @Override
    public String mapCountry(String publicId, String graphiteCountry, String plantId) {
        String equivalencia =null;
        equivalencia = catalogManagerCacheService.getEquivalencia(
                XMLConstants.CATALOG_COUNTRY, graphiteCountry,plantId);

        if(equivalencia == null) {
            log.warn("[CATALOG-MISSING] supplier={} businessUnit={} catalog={} sourceCode={} lookupKey=({},{},{}) resolvedValue=NULL",
                    publicId, plantId, XMLConstants.CATALOG_COUNTRY, graphiteCountry,
                    XMLConstants.CATALOG_COUNTRY, graphiteCountry, plantId);
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_COUNTRY,
                    graphiteCountry,
                    plantId,
                    "graphite"
            );
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
            log.warn("[CATALOG-MISSING] supplier={} businessUnit={} catalog={} sourceCode={} lookupKey=({},{},{}) resolvedValue=NULL",
                    publicId, plantId, XMLConstants.CATALOG_COUNTRY_INTEGITY, graphiteCountry,
                    XMLConstants.CATALOG_COUNTRY_INTEGITY, graphiteCountry, plantId);
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_COUNTRY_INTEGITY,
                    graphiteCountry,
                    plantId,
                    "Integrity"
            );
        }else {
            log.info("Equivalencia encontrada para country='{}' y plantId='{}': '{}'", graphiteCountry, plantId, equivalencia);
        }
        return equivalencia;
    }
    @Override
    public UpdateInfo resolveUpdateInfo(SuppliersRowEntity supplier) {

        String businessUnit = supplier.getBusinessUnitCode();
        String supplierCode = supplier.getSupplierCode();

        boolean existsInQad = suppliersRowRepository.existsBySupplierCodeAndBusinessUnitCodeAndXmlStatusIn(
                supplierCode,
                businessUnit,
                java.util.List.of(com.rassini.graphite_client.entity.XmlStatus.GENERATED, com.rassini.graphite_client.entity.XmlStatus.GENERATED_PREV)
        );

        String partialUpdate = existsInQad ? XMLConstants.TRUE : XMLConstants.FALSE;
        String activityCode = existsInQad ? XMLConstants.MODIFY : XMLConstants.CREATE;

        log.info("[QAD-EVAL] supplier={} businessUnit={} rowId={} existsInQad={} tcActivityCode={} tlPartialUpdate={}",
                supplierCode, businessUnit, supplier.getId(), existsInQad, activityCode, partialUpdate);

        return UpdateInfo.builder()
                .partialUpdate(partialUpdate)
                .activityCode(activityCode)
                .build();
    }


    @Override
    public String getActivityCode(SuppliersRowEntity supplier) {
        String businessUnit = supplier.getBusinessUnitCode();
        String supplierCode = supplier.getSupplierCode();

        boolean existsInQad = suppliersRowRepository.existsBySupplierCodeAndBusinessUnitCodeAndXmlStatusIn(
                supplierCode,
                businessUnit,
                java.util.List.of(com.rassini.graphite_client.entity.XmlStatus.GENERATED, com.rassini.graphite_client.entity.XmlStatus.GENERATED_PREV)
        );

        String activityCode = existsInQad ? XMLConstants.MODIFY : XMLConstants.CREATE;

        log.info("[QAD-EVAL] getActivityCode supplier={} businessUnit={} rowId={} existsInQad={} tcActivityCode={}",
                supplierCode, businessUnit, supplier.getId(), existsInQad, activityCode);

        return activityCode;
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
            log.warn("[CATALOG-MISSING] supplier={} businessUnit={} catalog={} sourceCode={} lookupKey=({},{},{}) resolvedValue=NULL",
                    publicId, plantId, XMLConstants.CATALOG_STATE, graphiteState,
                    XMLConstants.CATALOG_STATE, graphiteState, plantId);
            catalogEquivalenciaFaltanteService.registrar(
                    publicId,
                    XMLConstants.CATALOG_STATE,
                    graphiteState,
                    plantId,
                    "graphite"
            );
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
        String businessUnit = supplier.getBusinessUnitCode();
        String supplierCode = supplier.getSupplierCode();

        boolean existsInQad = suppliersRowRepository.existsBySupplierCodeAndBusinessUnitCodeAndXmlStatusIn(
                supplierCode,
                businessUnit,
                java.util.List.of(com.rassini.graphite_client.entity.XmlStatus.GENERATED, com.rassini.graphite_client.entity.XmlStatus.GENERATED_PREV)
        );

        String action = existsInQad ? XMLConstants.MODIFY : XMLConstants.SAVE;

        log.info("[QAD-EVAL] getAction supplier={} businessUnit={} rowId={} existsInQad={} tcAction={}",
                supplierCode, businessUnit, supplier.getId(), existsInQad, action);

        return action;
    }
    
}
