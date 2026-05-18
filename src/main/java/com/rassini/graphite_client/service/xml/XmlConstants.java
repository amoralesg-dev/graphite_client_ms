package com.rassini.graphite_client.service.xml;

public final class XmlConstants {

    private XmlConstants() {
        // prevent instantiation
    }

    public static final String TEMPLATE_BASE = "templates/";

    // Base paths
    
    public static final String OUTPUT="C:\\temp";
 

    public static final String OUTPUT_BASE_XML = OUTPUT+"\\xml";
    public static final String OUTPUT_BASE_INTEGRITY = OUTPUT+"\\integrity";


    public static final String OUTPUT_PN_DIR          = OUTPUT_BASE_XML+"\\PN";
    public static final String OUTPUT_OC_DIR          = OUTPUT_BASE_XML+"\\OCBYP";
    public static final String OUTPUT_FRENOS_DIR      = OUTPUT_BASE_XML+"\\FRENOS";
    public static final String OUTPUT_BREAKES_DIR     = OUTPUT_BASE_XML+"\\BREAKES";


    // Templates FRENOS
    public static final String TEMPLATE_FRENOS_CREDITOR = TEMPLATE_BASE + "frenos/creditor.xml";
    public static final String TEMPLATE_FRENOS_BUSREL   = TEMPLATE_BASE + "frenos/busrel.xml";

    // Templates BREAKES
    public static final String TEMPLATE_BREAKES_CREDITOR = TEMPLATE_BASE + "breakes/creditor.xml";
    public static final String TEMPLATE_BREAKES_BUSREL   = TEMPLATE_BASE + "breakes/busrel.xml";


    
    // Templates OC
    public static final String TEMPLATE_OC_BUSREL = TEMPLATE_BASE + "oc/busrel.xml";
    public static final String TEMPLATE_OC_CREDITOR = TEMPLATE_BASE + "oc/creditor.xml";


    
    // Templates PN
    public static final String TEMPLATE_PN_BUSREL = TEMPLATE_BASE + "pn/busrel.xml";
    public static final String TEMPLATE_PN_CREDITOR = TEMPLATE_BASE + "pn/creditor.xml";


}
