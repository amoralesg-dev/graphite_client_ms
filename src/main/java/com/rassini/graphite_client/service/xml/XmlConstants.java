package com.rassini.graphite_client.service.xml;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class XmlConstants {

    private XmlConstants() {
        // prevent instantiation
    }

    public static final String TEMPLATE_BASE = "templates";

    // Base paths
    public static final String OUTPUT = resolveOutputBase();
    public static final String OUTPUT_BASE_XML = buildPath(OUTPUT, "xml");
    public static final String OUTPUT_BASE_INTEGRITY = buildPath(OUTPUT, "integrity");

    public static final String OUTPUT_PN_DIR = buildPath(OUTPUT_BASE_XML, "PN");
    public static final String OUTPUT_OC_DIR = buildPath(OUTPUT_BASE_XML, "OCBYP");
    public static final String OUTPUT_FRENOS_DIR = buildPath(OUTPUT_BASE_XML, "FRENOS");
    public static final String OUTPUT_BREAKES_DIR = buildPath(OUTPUT_BASE_XML, "BREAKES");

    // Templates FRENOS
    public static final String TEMPLATE_FRENOS_CREDITOR = buildPath(TEMPLATE_BASE, "frenos", "creditor.xml");
    public static final String TEMPLATE_FRENOS_BUSREL = buildPath(TEMPLATE_BASE, "frenos", "busrel.xml");

    // Templates BREAKES
    public static final String TEMPLATE_BREAKES_CREDITOR = buildPath(TEMPLATE_BASE, "breakes", "creditor.xml");
    public static final String TEMPLATE_BREAKES_BUSREL = buildPath(TEMPLATE_BASE, "breakes", "busrel.xml");

    // Templates OC
    public static final String TEMPLATE_OC_BUSREL = buildPath(TEMPLATE_BASE, "oc", "busrel.xml");
    public static final String TEMPLATE_OC_CREDITOR = buildPath(TEMPLATE_BASE, "oc", "creditor.xml");

    // Templates PN
    public static final String TEMPLATE_PN_BUSREL = buildPath(TEMPLATE_BASE, "pn", "busrel.xml");
    public static final String TEMPLATE_PN_CREDITOR = buildPath(TEMPLATE_BASE, "pn", "creditor.xml");

    private static String resolveOutputBase() {
        String configured =
                System.getProperty("XML_OUTPUT_PATH",
                        System.getenv("XML_OUTPUT_PATH"));

        if (configured != null && !configured.isBlank()) {
            return normalize(configured);
        }

        boolean isWindows = System.getProperty("os.name")
                .toLowerCase()
                .contains("win");

        return isWindows ? "C:/app/output" : "/app/output";
    }

    private static String buildPath(String first, String... more) {
        Path path = Paths.get(first, more);
        return normalize(path.toString());
    }

    private static String normalize(String path) {
        return path.replace("\\", "/");
    }
}