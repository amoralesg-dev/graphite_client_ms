package com.rassini.graphite_client.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rassini.graphite_client.dto.AckRequest;

public interface GraphiteApiClient {
    JsonNode getChanges(String interfaceName, boolean filterReady, String publicIds);
    JsonNode getProfile(String publicId, boolean applyRules);
    void acknowledgeChange(AckRequest ackRequest); 
}
