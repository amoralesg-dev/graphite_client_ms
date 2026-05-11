package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditorXmlContext {

    // Output
    private String outputFileName;

    // XML sections
    private ContextInfoXml contextInfo;
    private CreditorNodoXML creditor;
    
    
    
}
