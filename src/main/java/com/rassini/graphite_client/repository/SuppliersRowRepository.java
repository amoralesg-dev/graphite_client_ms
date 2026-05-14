package com.rassini.graphite_client.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rassini.graphite_client.entity.SuppliersRowEntity;

// Para suppliers
@Repository
public interface SuppliersRowRepository
        extends JpaRepository<SuppliersRowEntity, Long> {


        Optional<SuppliersRowEntity> findBySupplierCodeAndBusinessUnitCode(
                String supplierCode,
                String businessUnitCode
        );


       int countBySupplierCode(String supplierCode);


}
