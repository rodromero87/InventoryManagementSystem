package com.inventario.apicentral.domain.port.in;

import com.inventario.apicentral.shared.dto.InventoryDTO;

public interface GetInventoryQuery {

    InventoryDTO getBySku(String sku);
}
