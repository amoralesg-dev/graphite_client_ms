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
                "XML generado correctamente. Supplier={}, ERP={}",
                supplier.getCreditorCode(),
                supplier.getBusinessUnitCode()
            );

        } catch (Exception ex) {

            supplier.setXmlStatus(XmlStatus.ERROR);
            repository.save(supplier);

            log.error(
                "Error generando XML. Supplier={}, ERP={}",
                supplier.getCreditorCode(),
                supplier.getBusinessUnitCode(),
                ex
            );

            throw ex;
        }
    }

    /**
     * ✅ Idempotente por existencia de archivo:
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

            if (Files.exists(filePath)) {
                log.info(
                    "XML ya existe. Se omite. file={} Supplier={}, ERP={}",
                    filePath.toAbsolutePath(),
                    supplier.getCreditorCode(),
                    supplier.getBusinessUnitCode()
                );
                return;
            }

            xmlGenerationLogic.run();

            supplier.setXmlStatus(XmlStatus.GENERATED);
            repository.save(supplier);

            log.info(
                "XML generado correctamente. file={} Supplier={}, ERP={}",
                filePath.toAbsolutePath(),
                supplier.getCreditorCode(),
                supplier.getBusinessUnitCode()
            );

        } catch (Exception ex) {

            supplier.setXmlStatus(XmlStatus.ERROR);
            repository.save(supplier);

            log.error(
                "Error generando XML. Supplier={}, ERP={}",
                supplier.getCreditorCode(),
                supplier.getBusinessUnitCode(),
                ex
            );

            throw ex;
        }
    }
}