package com.inventario.apicentral.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataIdempotencyRepository extends JpaRepository<JpaIdempotencyEntity, String> {
}
