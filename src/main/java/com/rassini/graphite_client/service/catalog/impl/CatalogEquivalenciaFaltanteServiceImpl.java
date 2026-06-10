package com.rassini.graphite_client.service.catalog.impl;

import com.rassini.graphite_client.entity.CatalogEquivalenciaFaltanteEntity;
import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.repository.CatalogEquivalenciaFaltanteRepository;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.service.catalog.CatalogEquivalenciaFaltanteService;
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

    @Value("${mail.notification.to}")
    private String destinatariosConfig;

    public void registrar(String idCatalogo, String code, String businessUnit) {

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
                correo.setSubject("Equivalencia faltante detectada");
                correo.setBody(
                        "<html>" +
                                "<body>" +
                                "<h3>Se detectó una equivalencia faltante</h3>" +
                                "<p><b>Catálogo:</b> " + idCatalogo + "</p>" +
                                "<p><b>Code:</b> " + code + "</p>" +
                                "<p><b>Business Unit:</b> " + businessUnit + "</p>" +
                                "<p><b>Fecha detección:</b> " + entity.getFechaDeteccion() + "</p>" +
                                "</body>" +
                        "</html>"
                );

                correo.setEnviado(false);

                correoRepository.save(correo);
            });
    }
}