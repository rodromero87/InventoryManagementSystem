package com.inventario.loja.infrastructure.persistence;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LocalInventoryRepositoryAdapter {

    private final SpringDataLocalInventoryRepository jpa;

    public LocalInventoryRepositoryAdapter(SpringDataLocalInventoryRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<JpaLocalInventoryEntity> findForUpdate(String sku) {
        return jpa.findBySkuForUpdate(sku);
    }

    public JpaLocalInventoryEntity save(JpaLocalInventoryEntity e) {
        return jpa.saveAndFlush(e);
    }
}