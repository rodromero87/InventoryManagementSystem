package com.inventario.loja.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataLocalInventoryRepository extends JpaRepository<JpaLocalInventoryEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from JpaLocalInventoryEntity i where i.sku = :sku")
    Optional<JpaLocalInventoryEntity> findBySkuForUpdate(@Param("sku") String sku);
}