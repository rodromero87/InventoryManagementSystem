package com.inventario.loja.domain.port.in;

import com.inventario.loja.shared.dto.InventoryUpdatedEvent;

public interface ApplyRemoteEventUseCase {
    void apply(InventoryUpdatedEvent event);
}
