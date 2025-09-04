package com.inventario.loja.domain.port.out;

import com.inventario.loja.shared.dto.InventoryView;

import java.time.Instant;

public interface CentralInventoryClient {

    void adjust(String storeId, String sku, int delta, String idempotencyKey, long storeSeq, Instant occurredAt);

    InventoryView getInventory(String sku);
}
