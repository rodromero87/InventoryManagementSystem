package com.inventario.apicentral.infrastructure.events;

import com.inventario.apicentral.domain.port.out.EventPublisher;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publisher para desenvolvimento local sem AWS (apenas loga o payload).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryEventPublisher implements EventPublisher {

    @Override
    public void publishInventoryUpdated(InventoryDTO dto, String originStoreId, long originSeq) {
        var msg = InventoryUpdatedMessage.of(
                dto.sku(), dto.onHand(), dto.reserved(), dto.version(),
                originStoreId, originSeq, dto.updatedAt()
        );
        log.info("[EVENT in-memory] {}", msg);
    }
}
