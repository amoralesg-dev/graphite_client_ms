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

        Optional<SuppliersRowEntity> findBySupplierCodeAndBusinessUnitCodeAndAccountNumber(
               String supplierCode,
               String businessUnitCode,
               String accountNumber
         );



        int countByErpIdQadAndAccountNumber(String erpIdQad, String accountNumber);

        int countBySupplierCodeAndAccountNumber(String supplierCode, String accountNumber);


       int countByErpIdQad(String erpIdQad);

       List<SuppliersRowEntity> findAllByOrderBySupplierCodeAscIdAsc();

       List<SuppliersRowEntity> findAllByOrderByErpIdQadAscIdAsc();

       @Query("""
       SELECT s
       FROM SuppliersRowEntity s
       WHERE s.supplierCodeDisIntegrity IS NOT NULL
         AND TRIM(s.supplierCodeDisIntegrity) <> ''
       ORDER BY s.erpIdQad,
                s.supplierCodeDisIntegrity,
                s.id
       """)
      List<SuppliersRowEntity> findAllForIntegrityMigration();


      Optional<SuppliersRowEntity> findFirstBySupplierCodeAndAccountNumber(
               String supplierCode,
               String accountNumber);

      @Query("""
         select count(distinct s.accountNumber)
         from SuppliersRowEntity s
         where s.supplierCode = :supplierCode
      """)
      long countDistinctAccountsBySupplierCode(
            @Param("supplierCode") String supplierCode);


}
