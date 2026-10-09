package com.rassini.graphite_client.service.xml.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.entity.XmlStatus;
import com.rassini.graphite_client.repository.SuppliersRowRepository;

@Service
public class XmlGenerationHelper {

    private final SuppliersRowRepository repository;

    public XmlGenerationHelper(SuppliersRowRepository repository) {
        this.repository = repository;
    }

    /**
     * Genera el XML SIEMPRE (sin idempotencia).
     * Útil para pruebas o flujos donde no importa regenerar.
     */
    public void generate(
            SuppliersRowEntity supplier,
            Logger log,
            Runnable xmlGenerationLogic
    ) {
        try {
            xmlGenerationLogic.run();

            supplier.setXmlStatus(XmlStatus.GENERATED);
            repository.save(supplier);

            log.info(
                "XML generado correctamente. Supplier={}, ERP={}, ERP QAD={}",
                supplier.getSupplierCode(),
                supplier.getBusinessUnitCode(),
                supplier.getErpIdQad()
            );

        } catch (Exception ex) {

            supplier.setXmlStatus(XmlStatus.ERROR);
            repository.save(supplier);
            
            log.error(
                "Error generando XML. Supplier={}, ERP={}, ERP QAD={}",
                supplier.getSupplierCode(),
                supplier.getBusinessUnitCode(),
                supplier.getErpIdQad(),
                ex
            );

            throw ex;
        }
    }

    /**
     * Idempotente por existencia de archivo:
     * - Si el archivo ya existe y overwriteIfExists=false, NO se vuelve a generar (GENERATED_PREV).
     * - Si el archivo ya existe y overwriteIfExists=true (reproceso manual), se regenera y reemplaza de forma segura.
     * - Si no existe, se genera y se marca GENERATED.
     */
    public void generateIfFileNotExists(
            SuppliersRowEntity supplier,
            String outputDir,
            String outputFileName,
            Logger log,
            Runnable xmlGenerationLogic
    ) {
        generateIfFileNotExists(supplier, outputDir, outputFileName, false, log, xmlGenerationLogic);
    }

    public void generateIfFileNotExists(
            SuppliersRowEntity supplier,
            String outputDir,
            String outputFileName,
            boolean overwriteIfExists,
            Logger log,
            Runnable xmlGenerationLogic
    ) {
        try {
            Path targetDir = Paths.get(outputDir);
            Path filePath = targetDir.resolve(outputFileName);

            boolean fileExists = Files.exists(filePath);
            Path existingLegacyPath = null;

            if (!fileExists) {
                // Compatibilidad histórica: verificar si existe el nombre legacy duplicado equivalente
                String legacyDuplicateFileName = deriveLegacyDuplicateFileName(outputFileName, supplier.getBusinessUnitCode());
                if (legacyDuplicateFileName != null) {
                    Path legacyFilePath = targetDir.resolve(legacyDuplicateFileName);
                    if (Files.exists(legacyFilePath)) {
                        existingLegacyPath = legacyFilePath;
                    }
                }
            }

            boolean alreadyExists = fileExists || (existingLegacyPath != null);

            if (alreadyExists && !overwriteIfExists) {
                Path existing = fileExists ? filePath : existingLegacyPath;
                log.info(
                    "[XML-SKIP] mode=AUTOMATIC file={} Supplier={}, ERP={}, ERP QAD={}. Se respeta idempotencia y se omite generación.",
                    existing.toAbsolutePath(),
                    supplier.getSupplierCode(),
                    supplier.getBusinessUnitCode(),
                    supplier.getErpIdQad()
                );
                supplier.setXmlStatus(XmlStatus.GENERATED_PREV);
                repository.save(supplier);
                return;
            }

            if (alreadyExists && overwriteIfExists) {
                Path existingTarget = fileExists ? filePath : existingLegacyPath;
                log.info(
                    "[XML-OVERWRITE] mode=MANUAL_REPROCESS file={} Supplier={}, ERP={}, ERP QAD={}. Iniciando reemplazo seguro.",
                    existingTarget.toAbsolutePath(),
                    supplier.getSupplierCode(),
                    supplier.getBusinessUnitCode(),
                    supplier.getErpIdQad()
                );

                Path tempBackupPath = null;
                try {
                    Files.createDirectories(targetDir);
                    tempBackupPath = targetDir.resolve(outputFileName + ".tmp_bak_" + java.util.UUID.randomUUID());
                    java.nio.file.Files.move(existingTarget, tempBackupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Ejecutar la generación del nuevo XML en el destino normal
                    xmlGenerationLogic.run();

                    // Si la generación terminó con éxito, eliminar el respaldo temporal
                    try {
                        Files.deleteIfExists(tempBackupPath);
                    } catch (Exception exDel) {
                        log.warn("[XML-OVERWRITE] No se pudo eliminar el backup temporal {}", tempBackupPath, exDel);
                    }

                    supplier.setXmlStatus(XmlStatus.GENERATED);
                    repository.save(supplier);

                    log.info(
                        "[XML-OVERWRITE-SUCCESS] mode=MANUAL_REPROCESS file={} Supplier={}, ERP={}, ERP QAD={}. Reemplazo completado exitosamente.",
                        filePath.toAbsolutePath(),
                        supplier.getSupplierCode(),
                        supplier.getBusinessUnitCode(),
                        supplier.getErpIdQad()
                    );
                    return;

                } catch (Exception ex) {
                    // Falló la generación: restaurar archivo anterior intacto
                    log.error(
                        "[XML-OVERWRITE-FAILED] mode=MANUAL_REPROCESS file={} Supplier={}, ERP={}, ERP QAD={} previousFilePreserved=true: {}",
                        filePath.toAbsolutePath(),
                        supplier.getSupplierCode(),
                        supplier.getBusinessUnitCode(),
                        supplier.getErpIdQad(),
                        ex.getMessage(),
                        ex
                    );

                    if (tempBackupPath != null && Files.exists(tempBackupPath)) {
                        try {
                            try {
                                java.nio.file.Files.move(tempBackupPath, existingTarget, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                            } catch (java.io.IOException atomicEx) {
                                java.nio.file.Files.move(tempBackupPath, existingTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception restoreEx) {
                            log.error("[XML-OVERWRITE-RESTORE-ERROR] Error restaurando archivo original desde {}", tempBackupPath, restoreEx);
                        }
                    }

                    supplier.setXmlStatus(XmlStatus.ERROR);
                    repository.save(supplier);
                    throw ex;
                }
            }

            // Caso normal: archivo no existe aún
            xmlGenerationLogic.run();

            supplier.setXmlStatus(XmlStatus.GENERATED);
            repository.save(supplier);

            log.info(
                "XML generado correctamente. file={} Supplier={}, ERP={}, ERP QAD={}",
                filePath.toAbsolutePath(),
                supplier.getSupplierCode(),
                supplier.getBusinessUnitCode(),
                supplier.getErpIdQad()
            );

        } catch (Exception ex) {
            supplier.setXmlStatus(XmlStatus.ERROR);
            repository.save(supplier);

            log.error(
                "Error generando XML. Supplier={}, ERP={}, ERP QAD={}",
                supplier.getSupplierCode(),
                supplier.getBusinessUnitCode(),
                supplier.getErpIdQad(),
                ex
            );

            if (ex instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Error generando XML: " + ex.getMessage(), ex);
        }
    }

    private String deriveLegacyDuplicateFileName(String fileName, String bu) {
        if (fileName == null || bu == null || bu.isBlank()) return null;
        String suffix = "_" + bu + ".xml";
        if (fileName.endsWith(suffix)) {
            String base = fileName.substring(0, fileName.length() - 4); // quita .xml
            return base + "_" + bu + ".xml";
        }
        return null;
    }
}