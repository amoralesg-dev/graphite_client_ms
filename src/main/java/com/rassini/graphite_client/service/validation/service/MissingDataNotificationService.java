package com.rassini.graphite_client.service.validation.service;

import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import com.rassini.graphite_client.repository.CorreoPendienteRepository;
import com.rassini.graphite_client.service.validation.collector.MissingDataCollector;
import com.rassini.graphite_client.service.validation.model.MissingDataIssue;
import com.rassini.graphite_client.service.validation.model.OutputResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissingDataNotificationService {

    private final CorreoPendienteRepository correoPendienteRepository;

    @Value("${mail.notification.to:amoralesg@rassini.com,eguzman@rassini.com}")
    private String defaultRecipients;

    @Value("${spring.profiles.active:local}")
    private String environment;

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processAndNotify(MissingDataCollector collector, int totalProcessed) {
        if (collector == null || !collector.hasIssues()) {
            return;
        }

        List<MissingDataIssue> issues = collector.getIssues();
        if (issues.isEmpty()) {
            return;
        }

        try {
            long xmlManualCount = issues.stream().filter(i -> i.getResult() == OutputResult.GENERATED_MANUAL && i.getOutputType() == com.rassini.graphite_client.service.validation.model.OutputType.XML).count();
            long integrityManualCount = issues.stream().filter(i -> i.getResult() == OutputResult.GENERATED_MANUAL && i.getOutputType() == com.rassini.graphite_client.service.validation.model.OutputType.INTEGRITY).count();
            long notGeneratedCount = issues.stream().filter(i -> i.getResult() == OutputResult.NOT_GENERATED).count();

            long affectedSuppliersCount = issues.stream()
                    .map(MissingDataIssue::getSupplierCode)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .count();

            String subject = String.format("[INCIDENCIAS GRAPHITE] Datos faltantes detectados | Amb: %s | Prov: %d | No Gen: %d | Manual: %d",
                    environment, affectedSuppliersCount, notGeneratedCount, (xmlManualCount + integrityManualCount));

            String htmlBody = buildHtmlBody(issues, totalProcessed, affectedSuppliersCount, xmlManualCount, integrityManualCount, notGeneratedCount);

            CorreoPendienteEntity correo = new CorreoPendienteEntity();
            correo.setTo(defaultRecipients);
            correo.setSubject(subject);
            correo.setBody(htmlBody);
            correo.setEnviado(false);

            correoPendienteRepository.save(correo);
            log.info("[NOTIFICATION-SENT] Correo de incidencias registrado exitosamente en correo_pendiente con {} incidencias para {} proveedores",
                    issues.size(), affectedSuppliersCount);

        } catch (Exception e) {
            log.error("[NOTIFICATION-ERROR] Error al persistir CorreoPendienteEntity para incidencias de faltantes: {}", e.getMessage(), e);
        }
    }

    private String buildHtmlBody(List<MissingDataIssue> issues, int totalProcessed, long affectedSuppliers,
                                  long xmlManual, long integrityManual, long notGenerated) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><style>")
          .append("body { font-family: Arial, sans-serif; font-size: 13px; color: #333; }")
          .append(".summary-box { background-color: #f7f9fa; border: 1px solid #dcdfe6; padding: 12px; margin-bottom: 20px; border-radius: 4px; }")
          .append("table { border-collapse: collapse; width: 100%; margin-top: 10px; font-size: 12px; }")
          .append("th, td { border: 1px solid #ddd; padding: 7px 10px; text-align: left; }")
          .append("th { background-color: #f2f2f2; color: #111; font-weight: bold; }")
          .append(".tag-blocking { color: #d9534f; font-weight: bold; }")
          .append(".tag-warning { color: #f0ad4e; font-weight: bold; }")
          .append(".tag-manual { color: #0275d8; }")
          .append("</style></head><body>");

        sb.append("<h2>Reporte de Datos Faltantes e Incidencias en Generación</h2>");
        sb.append("<div class='summary-box'>");
        sb.append("<p><b>Ambiente:</b> ").append(environment).append(" | <b>Fecha/Hora:</b> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");
        sb.append("<p><b>Total Proveedores Procesados en Corrida:</b> ").append(totalProcessed).append("</p>");
        sb.append("<p><b>Proveedores con Faltantes:</b> ").append(affectedSuppliers).append("</p>");
        sb.append("<p><b>Archivos XML enviados a Manual:</b> ").append(xmlManual).append("</p>");
        sb.append("<p><b>Archivos Integrity enviados a Manual:</b> ").append(integrityManual).append("</p>");
        sb.append("<p><b>Archivos que NO pudieron generarse:</b> ").append(notGenerated).append("</p>");
        sb.append("</div>");

        // Agrupación por Proveedor -> Unidad -> Tipo Salida
        Map<String, List<MissingDataIssue>> bySupplier = issues.stream()
                .collect(Collectors.groupingBy(i -> i.getSupplierCode() != null ? i.getSupplierCode() : "DESCONOCIDO"));

        for (Map.Entry<String, List<MissingDataIssue>> supEntry : bySupplier.entrySet()) {
            sb.append("<h3>Proveedor: ").append(supEntry.getKey()).append("</h3>");

            sb.append("<table>");
            sb.append("<tr>")
              .append("<th>Unidad</th>")
              .append("<th>ERP/QAD</th>")
              .append("<th>Salida</th>")
              .append("<th>Subtipo</th>")
              .append("<th>Cuenta</th>")
              .append("<th>Campo Faltante</th>")
              .append("<th>Nodo Esperado</th>")
              .append("<th>Nodo Alterno</th>")
              .append("<th>Severidad</th>")
              .append("<th>Resultado</th>")
              .append("<th>Motivo / Detalle Técnico</th>")
              .append("</tr>");

            for (MissingDataIssue issue : supEntry.getValue()) {
                sb.append("<tr>");
                sb.append("<td>").append(issue.getBusinessUnitCode() != null ? issue.getBusinessUnitCode() : "-").append("</td>");
                sb.append("<td>").append(issue.getErpIdQad() != null ? issue.getErpIdQad() : "-").append("</td>");
                sb.append("<td>").append(issue.getOutputType()).append("</td>");
                sb.append("<td>").append(issue.getSubType() != null ? issue.getSubType() : "-").append("</td>");
                sb.append("<td>").append(issue.getMaskedAccountNumber() != null ? issue.getMaskedAccountNumber() : "N/A").append("</td>");
                sb.append("<td><b>").append(issue.getFieldName() != null ? issue.getFieldName() : "-").append("</b></td>");
                sb.append("<td>").append(issue.getExpectedNode() != null ? issue.getExpectedNode() : "-").append("</td>");
                sb.append("<td>").append(issue.getAlternateNodeFound() != null ? issue.getAlternateNodeFound() : "-").append("</td>");

                String sevClass = issue.getSeverity() == com.rassini.graphite_client.service.validation.model.IssueSeverity.BLOCKING ? "tag-blocking" : "tag-warning";
                sb.append("<td class='").append(sevClass).append("'>").append(issue.getSeverity()).append("</td>");

                sb.append("<td>").append(issue.getResult()).append("</td>");
                sb.append("<td>").append(issue.getTechnicalMessage() != null ? issue.getTechnicalMessage() : "").append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table><br/>");
        }

        sb.append("<p style='font-size: 11px; color: #777;'>Aviso: Este mensaje contiene números de cuenta enmascarados de acuerdo a las directivas de seguridad.</p>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
