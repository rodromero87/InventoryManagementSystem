package com.inventario.apicentral.application;

import com.inventario.apicentral.domain.port.in.GetInventoryQuery;
import com.inventario.apicentral.domain.port.out.InventoryRepository;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GetInventoryService implements GetInventoryQuery {

    private final InventoryRepository repo;
    public GetInventoryService(InventoryRepository repo){ this.repo = repo; }

    @Transactional(readOnly = true)
    @Override
    public InventoryDTO getBySku(String sku) {
        return repo.findBySkuForUpdate(sku)
                .map(i -> new InventoryDTO(i.sku(), i.onHand(), i.reserved(), i.version(), i.updatedAt()))
                .orElseGet(() -> new InventoryDTO(sku, 0, 0, 0, Instant.EPOCH));
    }
}
