package com.rassini.graphite_client.service.sync;

public interface GraphiteSyncService {
    void executeFullSync();
    void syncSpecificSuppliers(String publicIds);

}
