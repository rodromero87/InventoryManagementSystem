package com.inventario.loja.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppliedEventRepository extends JpaRepository<JpaAppliedEventEntity, String> {}
