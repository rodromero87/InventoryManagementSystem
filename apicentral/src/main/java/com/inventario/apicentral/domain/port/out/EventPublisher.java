package com.inventario.apicentral.domain.port.out;

import com.inventario.apicentral.shared.dto.InventoryDTO;

public interface EventPublisher {

    void publishInventoryUpdated(InventoryDTO dto, String originStoreId, long originSeq);
}
