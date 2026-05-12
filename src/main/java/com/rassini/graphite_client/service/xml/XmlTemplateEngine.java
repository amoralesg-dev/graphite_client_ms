package com.rassini.graphite_client.service.xml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.rassini.graphite_client.service.xml.context.*;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class XmlTemplateEngine {

    private final XPath xpath = XPathFactory.newInstance().newXPath();

    // ======================================================
    // BUSREL
    // ======================================================
    public void generateBusinessRelationXml(
            String templatePath,
            String outputDir,
            XmlContext ctx
    ) {

        log.info(
                "[XML-ENGINE] BUSREL | template={} | dir={} | file={}",
                templatePath,
                outputDir,
                ctx.getOutputFileName()
        );

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IllegalStateException("Template no encontrado: " + templatePath);
            }

            Document doc = newDocument(is);

            // 1) ContextInfo
            applySection(doc, "/BBusinessRelation/tContextInfo", ctx.getContextInfo());

            // 2) BusinessRelation
            applySection(doc, "/BBusinessRelation/tBusinessRelation", ctx.getBusinessRelation());

            // 3) Address
            applySection(doc, "/BBusinessRelation/tBusinessRelation/tAddress", ctx.getAddress());

            // 4) Contact
            applySection(doc, "/BBusinessRelation/tBusinessRelation/tAddress/tContact", ctx.getContact());

            write(doc, outputDir, ctx.getOutputFileName());

        } catch (Exception e) {
            log.error("[XML] Error generando BUSREL XML", e);
            throw new IllegalStateException("Error generando BUSREL XML", e);
        }
    }

    // ======================================================
    // CREDITOR
    // ======================================================
    public void generateCreditorXml(
            String templatePath,
            String outputDir,
            CreditorXmlContext ctx
    ) {

        log.info(
                "[XML-ENGINE] CREDITOR | template={} | dir={} | file={}",
                templatePath,
                outputDir,
                ctx.getOutputFileName()
        );

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IllegalStateException("Template no encontrado: " + templatePath);
            }

            Document doc = newDocument(is);

            // 1) ContextInfo (segmentado)
            applySection(doc, "/BCreditor/tContextInfo", ctx.getContextInfo());

            // 2) Creditor (✅ AQUÍ está la corrección)
            // Los campos fijos viven en ctx.getCreditor() (CreditorNodoXML)
            applySection(doc, "/BCreditor/tCreditor", ctx.getCreditor());

            write(doc, outputDir, ctx.getOutputFileName());

        } catch (Exception e) {
            log.error("[XML] Error generando CREDITOR XML", e);
            throw new IllegalStateException("Error generando CREDITOR XML", e);
        }
    }

    // ======================================================
    // Aplicación genérica de secciones (seteando TODO field)
    // ======================================================
    private void applySection(
        Document doc,
        String baseXPath,
        Object sectionObj
) throws Exception {

    // ===============================
    // 1) Si no hay objeto, no hay nada que aplicar
    // ===============================
    if (sectionObj == null) {
        return;
    }

    // ===============================
    // 2) Recorremos SOLO los fields declarados del objeto
    //    (ContextInfoXml, BusinessRelationXml, AddressXml, ContactXml, etc.)
    // ===============================
    for (Field f : sectionObj.getClass().getDeclaredFields()) {

        // ===============================
        // 3) Ignorar campos técnicos de Java
        // ===============================
        if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
            continue;
        }

        // ===============================
        // 4) Evitar campos internos que NO son XML
        // ===============================
        if ("outputFileName".equals(f.getName())) {
            continue;
        }

        // ===============================
        // 5) Acceso al valor del field
        // ===============================
        f.setAccessible(true);
        Object raw = f.get(sectionObj);

        // ===============================
        // 6) NORMALIZACIÓN DE VALOR
        //
        // Reglas:
        // - null          → NO se pinta
        // - ""            → NO se pinta
        // - "NULL"        → NO se pinta (para xsi:nil del template)
        //
        // Esto evita:
        // - sobrescribir nodos del template
        // - "ensuciar" nodos Custom*
        // ===============================
        if (raw == null) {
            continue;
        }

        String value = String.valueOf(raw);
        if (value.isBlank() || XMLConstants.NULL.equalsIgnoreCase(value)) {
            continue;
        }

        // ===============================
        // 7) Field Java → Tag XML
        // ===============================
        String tag = toXmlTag(f.getName());

        // ===============================
        // 8) SETEO SEGURO:
        //    - Busca el nodo EXISTENTE en el template
        //    - SI EXISTE → setTextContent
        //    - SI NO EXISTE → NO hace nada
        //
        // 🔒 Esto garantiza que:
        //    - el orden del template NO cambia
        //    - no se crean nodos nuevos
        // ===============================
        setIfExists(doc, baseXPath + "/" + tag, value);
    }
}
    // ======================================================
    // XML helpers
    // ======================================================
    private Document newDocument(InputStream is) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(is);
    }

    private void write(Document doc, String outputDir, String fileName) throws Exception {
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        Path out = dir.resolve(fileName);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(new DOMSource(doc), new StreamResult(out.toFile()));

        log.info("[XML] Generado: {}", out.toAbsolutePath());
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

    // ======================================================
    // Field → Tag (corregido)
    // ======================================================
    private String toXmlTag(String fieldName) {

        if (fieldName.contains("_")) {
            return fieldName; // tc_Rowid, tc_ParentRowid, etc.
        }

        if (fieldName.startsWith("businessRelation")) {
            return "BusinessRelation" + fieldName.substring("businessRelation".length());
        }

        if (fieldName.startsWith("address")) {
            return "Address" + fieldName.substring("address".length());
        }

        if (fieldName.startsWith("contact")) {
            return "Contact" + fieldName.substring("contact".length());
        }

        if (fieldName.startsWith("creditor")) {
            return "Creditor" + fieldName.substring("creditor".length());
        }

        if (fieldName.startsWith("lastModified")) {
            return "LastModified" + fieldName.substring("lastModified".length());
        }

        // ✅ tc/ti/tt/tl se mantienen tal cual (minúscula)
        if (fieldName.startsWith("tc")
                || fieldName.startsWith("ti")
                || fieldName.startsWith("tt")
                || fieldName.startsWith("tl")) {
            return fieldName;
        }

        // ✅ tx* en template va con "T" mayúscula: TxzTaxZone, TxclTaxCls, TxuTaxUsage
        if (fieldName.startsWith("tx")) {
            return "T" + fieldName.substring(1);
        }

        if (fieldName.startsWith("custom")) {
            return "Custom" + capitalize(fieldName.substring("custom".length()));
        }

        if (fieldName.startsWith("qAD") || fieldName.startsWith("qad")) {
            return fieldName.toUpperCase();
        }

        return capitalize(fieldName);
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void setIfExists(
        Document doc,
        String simplePath,
        String value
    ) throws XPathExpressionException {

        Node node = (Node) xpath.evaluate(
                toLocalNameXPath(simplePath),
                doc,
                XPathConstants.NODE
        );

        if (node != null) {
            node.setTextContent(value);
        }
    }
}