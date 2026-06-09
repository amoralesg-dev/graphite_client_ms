package com.rassini.graphite_client.repository;

import com.rassini.graphite_client.entity.CorreoPendienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorreoPendienteRepository extends JpaRepository<CorreoPendienteEntity, Long> {
    List<CorreoPendienteEntity> findByEnviadoFalse();
}
