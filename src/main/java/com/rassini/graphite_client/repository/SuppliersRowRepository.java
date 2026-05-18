package com.rassini.graphite_client.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rassini.graphite_client.entity.SuppliersRowEntity;

// Para suppliers
@Repository
public interface SuppliersRowRepository
        extends JpaRepository<SuppliersRowEntity, Long> {


        
        
        @Query("SELECT s " +
           "FROM SuppliersRowEntity s " +
           "WHERE s.id IN (" +
           "   SELECT MIN(sr.id) " +
           "   FROM SuppliersRowEntity sr " +
           "   WHERE sr.supplierCodeDisIntegrity = :supplierCode " +
           "   GROUP BY sr.accountNumber" +
           ")")
        List<SuppliersRowEntity> findDistinctAccountsBySupplierCode(@Param("supplierCode") String supplierCode);


        @Query("SELECT s " +
           "FROM SuppliersRowEntity s " +
           "WHERE s.id IN (" +
           "   SELECT MIN(sr.id) " +
           "   FROM SuppliersRowEntity sr " +
           "   WHERE sr.erpIdQad = :erpIdQad " +
           "   GROUP BY sr.accountNumber" +
           ")")
        List<SuppliersRowEntity> findDistinctAccountsByErpIdQad(@Param("erpIdQad") String erpIdQad);


        Optional<SuppliersRowEntity> findBySupplierCodeAndBusinessUnitCode(
                String supplierCode,
                String businessUnitCode
        );


       int countBySupplierCode(String supplierCode);


}
