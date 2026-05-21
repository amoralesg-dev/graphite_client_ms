package com.rassini.graphite_client.repository;

import com.rassini.graphite_client.entity.CatalogManagerEntity;
import com.rassini.graphite_client.entity.CatalogManagerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogManagerRepository extends JpaRepository<CatalogManagerEntity, CatalogManagerId> {
}