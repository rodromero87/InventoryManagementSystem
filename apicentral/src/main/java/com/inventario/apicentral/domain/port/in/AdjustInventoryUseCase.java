package com.inventario.apicentral.domain.port.in;

import com.inventario.apicentral.shared.dto.AdjustCommand;
import com.inventario.apicentral.shared.dto.InventoryDTO;

public interface AdjustInventoryUseCase {

    InventoryDTO adjust(AdjustCommand c);
}
