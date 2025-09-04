package com.inventario.loja.domain.port.out;

import com.inventario.loja.domain.model.LocalInventory;

import java.util.Optional;

public interface LocalInventoryRepository {
    Optional<LocalInventory> findForUpdate(String sku);
    LocalInventory save(LocalInventory inv);
}
