package com.rassini.graphite_client.service.xml.impl;

import org.springframework.stereotype.Service;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;
import com.rassini.graphite_client.entity.SuppliersRowEntity;
import com.rassini.graphite_client.repository.SuppliersRowRepository;
import com.rassini.graphite_client.service.xml.CatalogService;
import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.XmlOcService;
import com.rassini.graphite_client.service.xml.XmlTemplateEngine;
import com.rassini.graphite_client.service.xml.context.CreditorXmlContext;
import com.rassini.graphite_client.service.xml.context.XmlContext;
import com.rassini.graphite_client.service.xml.factory.OcXmlFactory;
import com.rassini.graphite_client.service.xml.helper.XmlGenerationHelper;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import com.rassini.graphite_client.entity.XmlStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlOcServiceImpl implements XmlOcService {

    private final CatalogService catalogService;
    private final XmlTemplateEngine xmlTemplateEngine;
    private final SuppliersRowRepository suppliersRowRepository;
    private final XmlGenerationHelper xmlGenerationHelper;
    private final com.rassini.graphite_client.service.validation.service.OutputValidationService outputValidationService;
    private final com.rassini.graphite_client.service.validation.service.ManualOutputPathResolver manualOutputPathResolver;
    private final com.rassini.graphite_client.service.validation.collector.MissingDataCollector missingDataCollector;

    /**
     * Orquestador por planta OC (0111 / 0301)
     */
    @Override
    public void generate(GraphiteSupplierDto dto , SupplierEntity supplierParameter) {

        if (dto == null || dto.getErpRecords() == null) {
            return;
        }

        OcXmlFactory factory = new OcXmlFactory(catalogService);

        dto.getErpRecords().stream()
            .filter(erp -> {
                String id = erp.getRassiniErpEntityId();
                return XMLConstants.OC.equals(id) || XMLConstants.BYPASA.equals(id);
            })
            .forEach(erp -> {

                String erpId = erp.getRassiniErpEntityId();
                log.info("[XML-PROCESS] supplier={} businessUnit={} generator=OC eligible=true", dto.getEntityPublicId(), erpId);

                Optional<SuppliersRowEntity> supplierOpt = suppliersRowRepository
                        .findFirstBySupplierCodeAndBusinessUnitCodeOrderByIdAsc(
                                dto.getEntityPublicId(),
                                erpId
                        );

                if (supplierOpt.isEmpty()) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=OC result=ERROR reason=NO_ROW_IN_DB",
                            dto.getEntityPublicId(), erpId);
                    outputValidationService.validateBusrel(null, erpId);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                    return;
                }

                SuppliersRowEntity supplier = supplierOpt.get();
                log.info("[XML-PROCESS-SELECTED-ROW] supplierCode={} businessUnit={} id={} accountNumber={} xmlStatus={}",
                        supplier.getSupplierCode(), erpId, supplier.getId(), supplier.getAccountNumber(), supplier.getXmlStatus());

                // Validación centralizada de datos
                boolean busrelValid = outputValidationService.validateBusrel(supplier, erpId);
                boolean creditorValid = outputValidationService.validateCreditor(supplier, erpId);

                boolean hasBlocking = missingDataCollector.hasBlockingIssues(dto.getEntityPublicId(), erpId, null);
                boolean hasWarning = missingDataCollector.hasWarningIssues(dto.getEntityPublicId(), erpId, null);

                if (hasBlocking) {
                    log.warn("[XML-PROCESS] supplier={} businessUnit={} result=NOT_GENERATED reason=BLOCKING_DATA_MISSING", dto.getEntityPublicId(), erpId);
                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                    return;
                }

                String targetDir = manualOutputPathResolver.resolveXmlOutputDir(erpId, hasWarning);
                log.info("[XML-PROCESS] supplier={} businessUnit={} targetDir={} hasWarning={}", dto.getEntityPublicId(), erpId, targetDir, hasWarning);

                try {
                    log.debug(
                    "[TAX-DEBUG] erpId={} taxClass='{}' taxZone={}",
                    erpId,
                    erp.getRassiniErpTaxClass(),
                    erp.getRassiniErpTaxZone()
                    );
                    log.debug(
                    "[TERMS-DEBUG] erpId={} ErpPaymentTerms='{}'",
                    erpId,
                    erp.getRassiniErpPaymentTerms()
                    );

                    // =====================================================
                    // BUSREL
                    // =====================================================
                    XmlContext busrelCtx =
                            factory.buildBusrelContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone()
                            );

                    xmlGenerationHelper.generateIfFileNotExists(
                            supplier,
                            targetDir,
                            busrelCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateBusinessRelationXml(
                                    XmlConstants.TEMPLATE_OC_BUSREL,
                                    targetDir,
                                    busrelCtx
                            )
                    );

                    // =====================================================
                    // CREDITOR
                    // =====================================================
                    CreditorXmlContext creditorCtx =
                            factory.buildCreditorContext(
                                    supplier,
                                    erpId,
                                    erp.getRassiniErpTaxClass(),
                                    erp.getRassiniErpTaxZone(),
                                    erp.getRassiniErpPaymentTerms()
                            );

                    xmlGenerationHelper.generateIfFileNotExists(
                            supplier,
                            targetDir,
                            creditorCtx.getOutputFileName(),
                            log,
                            () -> xmlTemplateEngine.generateCreditorXml(
                                    XmlConstants.TEMPLATE_OC_CREDITOR,
                                    targetDir,
                                    creditorCtx
                            )
                    );

                    log.info("[XML-PROCESS] supplier={} businessUnit={} result=GENERATED dir={} files=[{}, {}]",
                            dto.getEntityPublicId(), erpId, targetDir, busrelCtx.getOutputFileName(), creditorCtx.getOutputFileName());

                } catch (Exception e) {
                    log.error("[XML-PROCESS] supplier={} businessUnit={} generator=OC result=ERROR: {}",
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
                            .technicalMessage("Excepción durante generación XML OC/BYPASA: " + e.getMessage())
                            .rootCauseException(e.getClass().getName())
                            .build());

                    supplier.setXmlStatus(XmlStatus.ERROR);
                    suppliersRowRepository.save(supplier);
                    if (supplierParameter != null) {
                        supplierParameter.setStatus(XMLConstants.BYPASA.equals(erpId) ? ProviderState.ERRORMAPBYPASA : ProviderState.ERRORMAPOC);
                    }
                }
            });
    }


    @Override
    public void generateBusinessRelation(GraphiteSupplierDto dto) {
        // Se genera por planta dentro de generate()
    }

    @Override
    public void generateCreditor(GraphiteSupplierDto dto) {
        // Se genera por planta dentro de generate()
    }
}