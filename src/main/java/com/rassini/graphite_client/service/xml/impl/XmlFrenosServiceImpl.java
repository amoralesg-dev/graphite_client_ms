package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlFrenosService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.FrenosXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import com.rassini.graphite_client.entity.XmlStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlFrenosServiceImpl implements XmlFrenosService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;
    private final com.rassini.graphite_client.service.validation.service.OutputValidationService outputValidationService;
    private final com.rassini.graphite_client.service.validation.service.ManualOutputPathResolver manualOutputPathResolver;
    private final com.rassini.graphite_client.service.validation.collector.MissingDataCollector missingDataCollector;

    @Override
    public void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter) {
        generate(dto, supplierParameter, false);
    }

    @Override
    public void generate(GraphiteSupplierDto dto, SupplierEntity supplierParameter, boolean overwriteIfExists) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        FrenosXmlFactory factory = new FrenosXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> XMLConstants.FRENOS.equals(erp.getRassiniErpEntityId()))
            .forEach(erp -> {
                String erpId = XMLConstants.FRENOS;
                log.info("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS eligible=true", dto.getEntityPublicId(), erpId);

                Optional<SuppliersRowEntity> supplierOpt =
                        suppliersRowRepository
                                .findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc(
                                        dto.getEntityPublicId(),
                                        erpId
                                );

                if (supplierOpt.isEmpty()) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS result=ERROR reason=NO_ROW_IN_DB",
                            dto.getEntityPublicId(), erpId);
                    outputValidationService.validateBusrel(null, erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                    return;
                }

                SuppliersRowEntity supplier = supplierOpt.get();
                log.info("[XML-PROCESS-SELECTED-ROW] supplierCode={} businessUnit={} id={} accountNumber={} xmlStatus={}",
                        supplier.getSupplierCode(), erpId, supplier.getId(), supplier.getAccountNumber(), supplier.getXmlStatus());

                // Validación centralizada de datos
                boolean busrelValid = outputValidationService.validateBusrel(supplier, erpId);
                boolean creditorValid = outputValidationService.validateCreditor(supplier, erpId);

                boolean hasBlocking = missingDataCollector.hasBlockingIssues(dto.getEntityPublicId(), erpId, com.rassini.graphite_client.service.validation.model.OutputType.XML);
                boolean hasWarning = missingDataCollector.hasWarningIssues(dto.getEntityPublicId(), erpId, com.rassini.graphite_client.service.validation.model.OutputType.XML);

                if (hasBlocking) {
                    log.warn("[XML-PROCESS] supplier={} businessUnit={} result=NOT_GENERATED reason=BLOCKING_DATA_MISSING", dto.getEntityPublicId(), erpId);
                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                    return;
                }

                String targetDir = manualOutputPathResolver.resolveXmlOutputDir(erpId, hasWarning);
                log.info("[XML-PROCESS] supplier={} businessUnit={} targetDir={} hasWarning={}", dto.getEntityPublicId(), erpId, targetDir, hasWarning);

                try {
                    // =========================
                    // BUSREL FRENOS
                    // =========================
                    // Decisión Create/Modify única, calculada ANTES de generar cualquier archivo
                    com.rassini.graphite_client.dto.UpdateInfo updateInfo = catalogService.resolveUpdateInfo(supplier);

                    XmlContext busrelCtx =
                            factory.buildBusrelContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone(),
                                    updateInfo
                            );

                    if (overwriteIfExists) {
                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                targetDir,
                                busrelCtx.getOutputFileName(),
                                true,
                                log,
                                () -> xmlTemplateEngine.generateBusinessRelationXml(
                                        XmlConstants.TEMPLATE_FRENOS_BUSREL,
                                        targetDir,
                                        busrelCtx
                                )
                        );
                    } else {
                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                targetDir,
                                busrelCtx.getOutputFileName(),
                                log,
                                () -> xmlTemplateEngine.generateBusinessRelationXml(
                                        XmlConstants.TEMPLATE_FRENOS_BUSREL,
                                        targetDir,
                                        busrelCtx
                                )
                        );
                    }

                    // =========================
                    // CREDITOR FRENOS
                    // =========================
                    // Invocación alineada con FrenosXmlFactory (5 argumentos; ErpRecord no requerido)
                    CreditorXmlContext creditorCtx =
                            factory.buildCreditorContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone(),
                                    erp.getRassiniErpPaymentTerms(),
                                    updateInfo
                            );

                    if (overwriteIfExists) {
                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                targetDir,
                                creditorCtx.getOutputFileName(),
                                true,
                                log,
                                () -> xmlTemplateEngine.generateCreditorXml(
                                        XmlConstants.TEMPLATE_FRENOS_CREDITOR,
                                        targetDir,
                                        creditorCtx
                                )
                        );
                    } else {
                        xmlGenerationHelper.generateIfFileNotExists(
                                supplier,
                                targetDir,
                                creditorCtx.getOutputFileName(),
                                log,
                                () -> xmlTemplateEngine.generateCreditorXml(
                                        XmlConstants.TEMPLATE_FRENOS_CREDITOR,
                                        targetDir,
                                        creditorCtx
                                )
                        );
                    }

                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=GENERATED dir={} files=[{}, {}]",
                            dto.getEntityPublicId(), erpId, targetDir, busrelCtx.getOutputFileName(), creditorCtx.getOutputFileName());

                } catch (Exception e) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=FRENOS result=ERROR: {}",
                            dto.getEntityPublicId(), erpId, e.getMessage(), e);
                    missingDataCollector.recordIssue(com.rassini.graphite_client.service.validation.model.MissingDataIssue.builder()
                            .supplierCode(dto.getEntityPublicId())
                            .erpIdQad(supplier.getErpIdQad())
                            .businessUnitCode(erpId)
                            .outputType(com.rassini.graphite_client.service.validation.model.OutputType.XML)
                            .subType("xml_generation")
                            .issueType(com.rassini.graphite_client.service.validation.model.IssueType.GENERATION_EXCEPTION)
                            .severity(com.rassini.graphite_client.service.validation.model.IssueSeverity.BLOCKING)
                            .result(com.rassini.graphite_client.service.validation.model.OutputResult.NOT_GENERATED)
                            .technicalMessage("Excepción durante generación XML FRENOS: " + e.getMessage())
                            .rootCauseException(e.getClass().getName())
                            .build());

                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(ProviderState.ERRORMAPFRENOS);
                    }
                }
            });
    }
}