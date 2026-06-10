package com.rassini.graphite_client.scheduler;

import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
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
public class CorreoPendienteScheduler {

    private final CorreoPendienteRepository repository;
    private final EmailService emailService;

    @Scheduled(cron = "#{@environment.getProperty('spring.mail.cron')}")
    public void enviarCorreosPendientes() {

        List<CorreoPendienteEntity> pendientes = repository.findByEnviadoFalse();

        if (pendientes.isEmpty()) {
            log.info("No hay correos pendientes por enviar");
            return;
        }

        pendientes.forEach(correo -> {
            try {
                // Soporta múltiples destinatarios separados por coma
                String[] destinatarios = correo.getTo().split(",");

                emailService.enviarCorreo(destinatarios,
                        correo.getSubject(),
                        correo.getBody());

                correo.setEnviado(true);
                correo.setFechaEnvio(LocalDateTime.now());

            } catch (Exception e) {
                log.error("Error al enviar correo id {}: {}", correo.getId(), e.getMessage());
            }
        });

        repository.saveAll(pendientes);

        log.info("Se enviaron {} correos pendientes", pendientes.size());
    }
}
