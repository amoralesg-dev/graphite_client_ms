package com.rassini.graphite_client.service.sync;

import com.rassini.graphite_client.dto.GraphiteSupplierDto;
import com.rassini.graphite_client.dto.SupplierMigrationResponse;

public interface IntegrityService {
    void createFileSupplierSync(String supplierID);

    void createFileSupplierSync(String supplierPublicId, String dtoErpIdQad);

    void createFileSupplierSync(GraphiteSupplierDto dto);

    void populateSupplierCodeDisIntegrity();

    void createFileSupplierMigration();

    SupplierMigrationResponse createFileSupplierMigrationByErpIds(java.util.List<String> erpIds);
}
