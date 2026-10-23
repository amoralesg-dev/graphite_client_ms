package com.rassini.graphite_client.scheduler;

import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoPendienteScheduler {

    private final CorreoPendienteRepository repository;
    private final EmailService emailService;

    /**
     * Registro en memoria de destinatarios con formato inválido irrecuperable por id de correo_pendiente,
     * para evitar reintentos infinitos silenciosos mientras el valor en BD no sea corregido.
     */
    private final Map<Long, String> destinatariosInvalidosReportados = new ConcurrentHashMap<>();

    @Scheduled(cron = "#{@environment.getProperty('spring.mail.cron')}")
    public void enviarCorreosPendientes() {

        List<CorreoPendienteEntity> pendientes = repository.findByEnviadoFalse();

        if (pendientes.isEmpty()) {
            log.info("No hay correos pendientes por enviar");
            return;
        }

        List<CorreoPendienteEntity> enviadosEnLote = new ArrayList<>();
        int enviadosExitosos = 0;
        int fallidos = 0;
        int omitidosPorDestinatarioInvalidoPrevio = 0;

        for (CorreoPendienteEntity correo : pendientes) {
            Long correoId = correo.getId();
            String rawTo = correo.getTo();

            if (correoId != null && destinatariosInvalidosReportados.containsKey(correoId)) {
                String previousInvalidTo = destinatariosInvalidosReportados.get(correoId);
                if (Objects.equals(previousInvalidTo, rawTo)) {
                    omitidosPorDestinatarioInvalidoPrevio++;
                    log.warn("[EMAIL-SKIP-INVALID] correoPendienteId={} destinatario='{}' se mantiene pendiente (enviado=false) por dirección inválida previamente detectada; corríjase en BD para reintentar",
                            correoId, rawTo);
                    continue;
                } else {
                    destinatariosInvalidosReportados.remove(correoId);
                }
            }

            try {
                emailService.enviarCorreo(
                        correoId,
                        rawTo,
                        correo.getSubject(),
                        correo.getBody());

                correo.setEnviado(true);
                correo.setFechaEnvio(LocalDateTime.now());
                enviadosEnLote.add(correo);
                enviadosExitosos++;

                if (correoId != null) {
                    destinatariosInvalidosReportados.remove(correoId);
                }

            } catch (IllegalArgumentException e) {
                fallidos++;
                correo.setEnviado(false);
                correo.setFechaEnvio(null);
                if (correoId != null) {
                    destinatariosInvalidosReportados.put(correoId, rawTo == null ? "<NULL>" : rawTo);
                }
                log.error("Error de destinatario inválido al enviar correo id {}: {}", correoId, e.getMessage());
            } catch (Exception e) {
                fallidos++;
                correo.setEnviado(false);
                correo.setFechaEnvio(null);
                log.error("Error al enviar correo id {}: {}", correoId, e.getMessage());
            }
        }

        if (!enviadosEnLote.isEmpty()) {
            repository.saveAll(enviadosEnLote);
            log.info("Se enviaron {} correos pendientes", enviadosExitosos);
        }

        if (fallidos > 0 || omitidosPorDestinatarioInvalidoPrevio > 0) {
            log.warn("[EMAIL-BATCH-SUMMARY] totalPendientes={} enviadosExitosos={} fallidos={} omitidosInvalidoPrevio={}",
                    pendientes.size(), enviadosExitosos, fallidos, omitidosPorDestinatarioInvalidoPrevio);
        }
    }

    public void limpiarCacheDestinatariosInvalidos() {
        destinatariosInvalidosReportados.clear();
    }
}
