package com.inventario.loja.domain.port.in;

import com.inventario.loja.shared.dto.InventoryView;

public interface GetInventoryQuery {
    InventoryView getBySku(String sku);
}
