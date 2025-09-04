package com.inventario.apicentral.application;

import com.inventario.apicentral.domain.model.Inventory;
import com.inventario.apicentral.domain.port.in.AdjustInventoryUseCase;
import com.inventario.apicentral.domain.port.out.EventPublisher;
import com.inventario.apicentral.domain.port.out.IdempotencyStore;
import com.inventario.apicentral.domain.port.out.InventoryRepository;
import com.inventario.apicentral.shared.dto.AdjustCommand;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import com.inventario.apicentral.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdjustInventoryService implements AdjustInventoryUseCase {

    private final InventoryRepository repo;
    private final IdempotencyStore idempotency;
    private final EventPublisher publisher;

    public AdjustInventoryService(InventoryRepository repo,
                                  IdempotencyStore idempotency,
                                  EventPublisher publisher) {
        this.repo = repo;
        this.idempotency = idempotency;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public InventoryDTO adjust(AdjustCommand c) {
        if (idempotency.exists(c.idempotencyKey())) {
            return repo.findBySku(c.sku())
                    .map(this::toDto)
                    .orElseGet(() -> toDto(new Inventory(c.sku(), 0, 0, 0, Instant.EPOCH)));
        }

        var now = Instant.now();
        var current = repo.findBySkuForUpdate(c.sku())
                .orElse(new Inventory(c.sku(), 0, 0, 0, now));

        int newOnHand = current.onHand() + c.delta();
        int newAtp = newOnHand - current.reserved();
        if (newAtp < 0) {
            throw new BusinessRuleViolationException(
                    "Ajuste inválido: resultaria em ATP negativo para SKU " + c.sku());
        }

        var updated = current.withOnHand(newOnHand, now);
        var saved = repo.upsert(updated);

        idempotency.save(c.idempotencyKey());
        publisher.publishInventoryUpdated(toDto(saved), c.storeId(), c.storeSeq());

        return toDto(saved);
    }

    private InventoryDTO toDto(Inventory i) {
        return new InventoryDTO(i.sku(), i.onHand(), i.reserved(), i.version(), i.updatedAt());
    }
}