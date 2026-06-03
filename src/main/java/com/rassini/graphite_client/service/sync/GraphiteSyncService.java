package com.rassini.graphite_client.service.sync;

public interface GraphiteSyncService {
    void executeFullSync(String detonante);
    void syncSpecificSuppliers(String publicIds, String detonante);

}
