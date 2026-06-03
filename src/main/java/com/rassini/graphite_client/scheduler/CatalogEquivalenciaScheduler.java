package com.rassini.graphite_client.scheduler;

import com.rassini.graphite_client.entity.CatalogEquivalenciaFaltanteEntity;
import com.rassini.graphite_client.repository.CatalogEquivalenciaFaltanteRepository;
import com.rassini.graphite_client.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEquivalenciaScheduler {

    private final CatalogEquivalenciaFaltanteRepository repository;
    private final EmailService emailService;

    @Scheduled(cron = "#{@environment.getProperty('spring.mail.cron')}")
    public void enviarPendientes() {

        List<CatalogEquivalenciaFaltanteEntity> pendientes =
                repository.findByNotificadoFalse();

        if (pendientes.isEmpty()) {

            log.info(
                "No existen equivalencias pendientes");

            return;
        }

        StringBuilder body = new StringBuilder();

        body.append("Equivalencias faltantes detectadas\n\n");

        pendientes.forEach(item -> {

            body.append("Catalogo: ")
                    .append(item.getIdCatalogo())
                    .append("\n");

            body.append("Code: ")
                    .append(item.getCode())
                    .append("\n");

            body.append("Business Unit: ")
                    .append(item.getBusinessUnit())
                    .append("\n");

            body.append("Ocurrencias: ")
                    .append(item.getTotalOcurrencias())
                    .append("\n");

            body.append("---------------------------------\n");
        });

       // emailService.enviarCorreo("Catalog Manager - Equivalencias faltantes",body.toString());

        pendientes.forEach(item -> {

            item.setNotificado(true);
            item.setFechaNotificacion(
                    LocalDateTime.now());
        });

        repository.saveAll(pendientes);

        log.info(
            "Correo enviado con {} registros",
            pendientes.size());
    }
}