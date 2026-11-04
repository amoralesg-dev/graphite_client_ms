package com.rassini.graphite_client.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rassini.graphite_client.entity.SuppliersVerEntity;

// Para suppliers_ver (historial de versiones bancarias)
@Repository
public interface SuppliersVerRepository extends JpaRepository<SuppliersVerEntity, Long> {

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM SuppliersVerEntity v WHERE v.supplierId = :supplierId")
    int findMaxVersionNumber(@Param("supplierId") Long supplierId);

    List<SuppliersVerEntity> findBySupplierIdOrderByVersionNumberAsc(Long supplierId);
}
