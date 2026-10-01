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
     *  Idempotente por existencia de archivo:
     * - Si el archivo ya existe, NO se vuelve a generar.
     * - Si no existe, se genera y se marca GENERATED.
     */
    public void generateIfFileNotExists(
            SuppliersRowEntity supplier,
            String outputDir,
            String outputFileName,
            Logger log,
            Runnable xmlGenerationLogic
    ) {

        try {
            Path filePath = Paths.get(outputDir).resolve(outputFileName);

            // 1. Verificar si ya existe con el nombre corregido
            if (Files.exists(filePath)) {
                log.info(
                    "XML ya existe con nombre corregido. Se omite. file={} Supplier={}, ERP={}, ERP QAD={}",
                    filePath.toAbsolutePath(),
                    supplier.getSupplierCode(),
                    supplier.getBusinessUnitCode(),
                    supplier.getErpIdQad()
                );
                supplier.setXmlStatus(XmlStatus.GENERATED_PREV);
                repository.save(supplier);
                return;
            }

            // 2. Compatibilidad histórica: verificar si existe el nombre legacy duplicado equivalente
            String legacyDuplicateFileName = deriveLegacyDuplicateFileName(outputFileName, supplier.getBusinessUnitCode());
            if (legacyDuplicateFileName != null) {
                Path legacyFilePath = Paths.get(outputDir).resolve(legacyDuplicateFileName);
                if (Files.exists(legacyFilePath)) {
                    log.info(
                        "XML histórico con nombre duplicado ya existe. Se respeta idempotencia. legacyFile={} Supplier={}, ERP={}, ERP QAD={}",
                        legacyFilePath.toAbsolutePath(),
                        supplier.getSupplierCode(),
                        supplier.getBusinessUnitCode(),
                        supplier.getErpIdQad()
                    );
                    supplier.setXmlStatus(XmlStatus.GENERATED_PREV);
                    repository.save(supplier);
                    return;
                }
            }

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

            throw ex;
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