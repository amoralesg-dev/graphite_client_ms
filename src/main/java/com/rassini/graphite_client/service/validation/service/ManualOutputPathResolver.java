package com.rassini.graphite_client.service.validation.service;

import com.rassini.graphite_client.service.xml.XmlConstants;
import com.rassini.graphite_client.service.xml.impl.util.XMLConstants;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ManualOutputPathResolver {

    public String resolveXmlOutputDir(String businessUnit, boolean isManual) {
        if (!isManual) {
            if (XMLConstants.PN.equals(businessUnit)) return XmlConstants.OUTPUT_PN_DIR;
            if (XMLConstants.PN99.equals(businessUnit)) return XmlConstants.OUTPUT_PN99_DIR;
            if (XMLConstants.OC.equals(businessUnit) || XMLConstants.BYPASA.equals(businessUnit)) return XmlConstants.OUTPUT_OC_DIR;
            if (XMLConstants.FRENOS.equals(businessUnit)) return XmlConstants.OUTPUT_FRENOS_DIR;
            if (XMLConstants.BREAKES.equals(businessUnit)) return XmlConstants.OUTPUT_BREAKES_DIR;
            return Paths.get(XmlConstants.OUTPUT_BASE_XML, businessUnit).toString();
        } else {
            if (XMLConstants.PN.equals(businessUnit)) return XmlConstants.OUTPUT_PN_MANUAL_DIR;
            if (XMLConstants.PN99.equals(businessUnit)) return XmlConstants.OUTPUT_PN99_MANUAL_DIR;
            if (XMLConstants.OC.equals(businessUnit) || XMLConstants.BYPASA.equals(businessUnit)) return XmlConstants.OUTPUT_OC_MANUAL_DIR;
            if (XMLConstants.FRENOS.equals(businessUnit)) return XmlConstants.OUTPUT_FRENOS_MANUAL_DIR;
            if (XMLConstants.BREAKES.equals(businessUnit)) return XmlConstants.OUTPUT_BREAKES_MANUAL_DIR;
            return Paths.get(XmlConstants.OUTPUT_BASE_XML_MANUAL, businessUnit).toString();
        }
    }

    public Path resolveIntegrityOutputDir(String businessUnit, boolean isManual) {
        String bu = (businessUnit != null && !businessUnit.isBlank()) ? businessUnit : "UNKNOWN";
        if (isManual) {
            return Paths.get(XmlConstants.OUTPUT_BASE_INTEGRITY_MANUAL, bu);
        } else {
            return Paths.get(XmlConstants.OUTPUT_BASE_INTEGRITY, bu);
        }
    }
}
