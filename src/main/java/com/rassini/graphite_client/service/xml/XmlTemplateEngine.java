package com.rassini.graphite_client.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class XmlTemplateEngine {

    private final XPath xpath = XPathFactory.newInstance().newXPath();

    public void generateBusinessRelationXml(String templatePath, String outputDir, XmlContext ctx) {

        log.info(
                "[XML-ENGINE] Entrando engine | template={} | dir={} | file={}",
                templatePath,
                outputDir,
                ctx.getOutputFileName()
        );

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
            if (is == null) {
                throw new IllegalStateException("Template no encontrado: " + templatePath);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(is);

            // --- ContextInfo
            set(doc, "/BBusinessRelation/tContextInfo/tcCompanyCode", ctx.getTcCompanyCode());
            set(doc, "/BBusinessRelation/tContextInfo/tcActivityCode", "Create");

            // --- BusinessRelation
            set(doc, "/BBusinessRelation/tBusinessRelation/BusinessRelationCode", ctx.getBusinessRelationCode());
            set(doc, "/BBusinessRelation/tBusinessRelation/BusinessRelationName1", ctx.getEntityName20());
            set(doc, "/BBusinessRelation/tBusinessRelation/BusinessRelationName2", ctx.getEntityName20());
            set(doc, "/BBusinessRelation/tBusinessRelation/BusinessRelationName3", ctx.getEntityName20());
            set(doc, "/BBusinessRelation/tBusinessRelation/BusinessRelationSearchName", ctx.getEntityName20());
            set(doc, "/BBusinessRelation/tBusinessRelation/tcCorporateGroupCode", ctx.getTcCorporateGroupCode());
            set(doc, "/BBusinessRelation/tBusinessRelation/tcLngCode", "ls");

            // --- Auditoría
            set(doc, "/BBusinessRelation/tBusinessRelation/LastModifiedDate", ctx.getLastModifiedDate());
            set(doc, "/BBusinessRelation/tBusinessRelation/LastModifiedTime", ctx.getLastModifiedTime());
            set(doc, "/BBusinessRelation/tBusinessRelation/LastModifiedUser", ctx.getLastModifiedUser());

            // --- Address
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressStreet1", ctx.getAddressStreet1());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressStreet2", ctx.getAddressStreet2());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressStreet3", ctx.getAddressStreet3());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressZip", ctx.getAddressZip());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressCity", ctx.getAddressCity());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressName", ctx.getAddressName());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressSearchName", ctx.getAddressSearchName());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressEMail", ctx.getAddressEmail());

            // --- Tax
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/TxzTaxZone", ctx.getTxzTaxZone());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/TxclTaxCls", ctx.getTxclTaxCls());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressTaxIDFederal", ctx.getRfc());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/AddressTaxIDState", ctx.getRfcState());

            // --- Country/State
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tcStateCode", ctx.getTcStateCode());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tcCountryCode", ctx.getTcCountryCode());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tcStateDescription", ctx.getTcStateDescription());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tcCountryDescription", ctx.getTcCountryDescription());

            // --- Contact
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tContact/ContactName", ctx.getContactName());
            set(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tContact/ContactEmail", ctx.getContactEmail());

            write(doc, outputDir, ctx.getOutputFileName());

        } catch (Exception e) {
            log.error("[XML] Error generando BUSREL XML desde templatePath={} outputDir={}", templatePath, outputDir, e);
            throw new IllegalStateException("Error generando BUSREL XML", e);
        }
    }

    /**
     * ✅ NUEVO: Generar CREDITOR XML
     */
    public void generateCreditorXml(String templatePath, String outputDir, CreditorXmlContext ctx) {

        log.info(
                "[XML-ENGINE] Entrando engine | template={} | dir={} | file={}",
                templatePath,
                outputDir,
                ctx.getOutputFileName()
        );

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
            if (is == null) {
                throw new IllegalStateException("Template no encontrado: " + templatePath);
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(is);

            // --- ContextInfo
            set(doc, "/BCreditor/tContextInfo/tcCompanyCode", ctx.getTcCompanyCode());
            set(doc, "/BCreditor/tContextInfo/tcActivityCode", "Create");

            // --- Creditor core
            set(doc, "/BCreditor/tCreditor/CreditorCode", ctx.getCreditorCode());
            set(doc, "/BCreditor/tCreditor/tcCurrencyCode", ctx.getTcCurrencyCode());
            set(doc, "/BCreditor/tCreditor/tcNormalPaymentConditionCode", ctx.getTcNormalPaymentConditionCode());

            // --- GL Profiles
            set(doc, "/BCreditor/tCreditor/tcInvControlGLProfileCode", ctx.getTcInvControlGLProfileCode());
            set(doc, "/BCreditor/tCreditor/tcCnControlGLProfileCode", ctx.getTcCnControlGLProfileCode());
            set(doc, "/BCreditor/tCreditor/tcPrepayControlGLProfileCode", ctx.getTcPrepayControlGLProfileCode());
            set(doc, "/BCreditor/tCreditor/tcDivisionProfileCode", ctx.getTcDivisionProfileCode());

            // --- Auditoría (en CREDITOR va dentro de tCreditor en tus XML reales)
            set(doc, "/BCreditor/tCreditor/LastModifiedDate", ctx.getLastModifiedDate());
            set(doc, "/BCreditor/tCreditor/LastModifiedTime", ctx.getLastModifiedTime());
            set(doc, "/BCreditor/tCreditor/LastModifiedUser", ctx.getLastModifiedUser());

            write(doc, outputDir, ctx.getOutputFileName());

        } catch (Exception e) {
            log.error("[XML] Error generando CREDITOR XML desde templatePath={} outputDir={}", templatePath, outputDir, e);
            throw new IllegalStateException("Error generando CREDITOR XML", e);
        }
    }

    // ======================================================
    // Helpers
    // ======================================================

    private void write(Document doc, String outputDir, String fileName) throws Exception {
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        Path out = dir.resolve(fileName);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(out.toFile()));

        log.info("[XML] Generado: {}", out.toAbsolutePath());
        log.info("[XML] File exists = {}", Files.exists(out));
        log.info("[XML] File size = {}", Files.exists(out) ? Files.size(out) : -1);
    }

    private void set(Document doc, String simplePath, String value) throws XPathExpressionException {
        Node node = (Node) xpath.evaluate(toLocalNameXPath(simplePath), doc, XPathConstants.NODE);
        if (node != null) {
            node.setTextContent(value == null ? "" : value);
        }
    }

    private String toLocalNameXPath(String simplePath) {
        String[] parts = simplePath.split("/");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            sb.append("/*[local-name()='").append(p).append("']");
        }
        return sb.toString();
    }
}