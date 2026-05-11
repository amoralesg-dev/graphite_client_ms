package com.rassini.graphite_client.service.xml.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class XmlContext {

    // Output
    private String outputFileName;

    // XML sections
    private ContextInfoXml contextInfo;
    private BusinessRelationXml businessRelation;
    private AddressXml address;
    private ContactXml contact;

}
