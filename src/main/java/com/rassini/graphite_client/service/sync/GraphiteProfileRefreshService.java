package com.rassini.graphite_client.service.sync;

public interface GraphiteProfileRefreshService {
    boolean processAndSaveInternal(String publicId, String detonante);
}
