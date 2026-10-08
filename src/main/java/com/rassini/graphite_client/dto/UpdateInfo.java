package com.rassini.graphite_client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInfo {

    private String partialUpdate;
    private String activityCode;

    /**
     * tcAction derivado de la misma decisión que activityCode:
     * Create -> SAVE, cualquier otro -> Modify. Evita reconsultar existsInQad.
     */
    public String resolveAction() {
        return com.rassini.graphite_client.service.xml.impl.util.XMLConstants.CREATE.equalsIgnoreCase(activityCode)
                ? com.rassini.graphite_client.service.xml.impl.util.XMLConstants.SAVE
                : com.rassini.graphite_client.service.xml.impl.util.XMLConstants.MODIFY;
    }

}
