package com.rassini.graphite_client.service.catalog.impl;

import com.rassini.graphite_client.entity.CatalogEquivalenciaFaltanteEntity;
import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.repository.CatalogEquivalenciaFaltanteRepository;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.repository.SupplierRepository;
import com.rassini.graphite_client.service.catalog.CatalogEquivalenciaFaltanteService;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogEquivalenciaFaltanteServiceImpl
        implements CatalogEquivalenciaFaltanteService {

    private final CatalogEquivalenciaFaltanteRepository repository;
    private final CorreoPendienteRepository correoRepository;
    private final SupplierRepository supplierRepository;


    @Value("${mail.notification.to}")
    private String destinatariosConfig;

    @Transactional
    public void setStatusError(String publicId, String businessUnit) {

        SupplierEntity supplier = supplierRepository
                .findByPublicId(publicId)
                .orElse(null);

        if (supplier == null) {
            log.warn("No se encontró proveedor {} con estado DESCARGA", publicId);
            return;
        }
        if (XMLConstants.FRENOS.equals(businessUnit)) {
            supplier.setStatus(ProviderState.ERRORMAPFRENOS);
        } else if (XMLConstants.BREAKES.equals(businessUnit)) {
            supplier.setStatus(ProviderState.ERRORMAPBREAKES);
        } else if (XMLConstants.BYPASA.equals(businessUnit)) {
            supplier.setStatus(ProviderState.ERRORMAPBYPASA);
        } else if (XMLConstants.OC.equals(businessUnit)) {
            supplier.setStatus(ProviderState.ERRORMAPOC);
        } else if (XMLConstants.PN.equals(businessUnit)) {
            supplier.setStatus(ProviderState.ERRORMAPPN);
        } else {
            supplier.setStatus(ProviderState.ERRORMAPPING);
        }
        SupplierEntity saved = supplierRepository.save(supplier);

        log.info(
            "Guardado proveedor {} con status {}",
            saved.getPublicId(),
            saved.getStatus()
        );
    }

    public void registrar(String publicId, String idCatalogo, String code, String businessUnit) {

        repository
            .findByIdCatalogoAndCodeAndBusinessUnitAndNotificado(
                idCatalogo,
                code,
                businessUnit,
                false)
            .ifPresentOrElse(entity -> {
                // Si ya existe, solo incrementa ocurrencias
                entity.setTotalOcurrencias(entity.getTotalOcurrencias() + 1);
                repository.save(entity);
                this.setStatusError(publicId,businessUnit);

            }, () -> {
                // Si no existe, crea nuevo registro
                CatalogEquivalenciaFaltanteEntity entity = new CatalogEquivalenciaFaltanteEntity();
                entity.setIdCatalogo(idCatalogo);
                entity.setCode(code);
                entity.setBusinessUnit(businessUnit);
                entity.setFechaDeteccion(LocalDateTime.now());
                entity.setTotalOcurrencias(1);
                entity.setNotificado(false);

                repository.save(entity);

                // Además, registra un correo pendiente
                CorreoPendienteEntity correo = new CorreoPendienteEntity();
                correo.setTo(destinatariosConfig); 
                correo.setSubject("Equivalencia faltante detectada proveedor "+publicId);
                correo.setBody(
                        "<html>" +
                                "<body>" +
                                "<h3>Se detectó una equivalencia faltante para proveedor <b>"+publicId+"</b></h3>" +
                                "<p><b>Catálogo:</b> " + idCatalogo + "</p>" +
                                "<p><b>Code:</b> " + code + "</p>" +
                                "<p><b>Business Unit:</b> " + businessUnit + "</p>" +
                                "<p><b>Fecha detección:</b> " + entity.getFechaDeteccion() + "</p>" +
                                "</body>" +
                        "</html>"
                );

                correo.setEnviado(false);
                this.setStatusError(publicId,businessUnit);

                correoRepository.save(correo);
            });
    }
}