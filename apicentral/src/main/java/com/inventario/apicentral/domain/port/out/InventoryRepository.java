package com.inventario.apicentral.domain.port.out;

import com.inventario.apicentral.domain.model.Inventory;

import java.util.Optional;

public interface InventoryRepository {

    Optional<Inventory> findBySku(String sku);

    Optional<Inventory> findBySkuForUpdate(String sku);

    Inventory upsert(Inventory inventory);
}
