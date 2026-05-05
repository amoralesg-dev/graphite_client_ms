package com.rassini.graphite_client.repository;

import com.rassini.graphite_client.entity.ProviderState;
import com.rassini.graphite_client.entity.SupplierEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, String> {

    
    Optional<SupplierEntity> findByPublicIdAndStatus(
            String publicId, ProviderState status
        );


}
