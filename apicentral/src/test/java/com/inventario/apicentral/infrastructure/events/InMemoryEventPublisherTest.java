package com.inventario.apicentral.infrastructure.events;

import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.junit.jupiter.api.Test;


import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InMemoryEventPublisherTest {

    @Test
    void publish_nao_deve_lancar_excecao_com_payload_valido() {
        var publisher = new InMemoryEventPublisher();
        var dto = new InventoryDTO("ABC-123", 50, 2, 5L, Instant.parse("2025-09-01T12:00:00Z"));

        assertDoesNotThrow(() ->
                publisher.publishInventoryUpdated(dto, "LOJA01", 42L)
        );
    }

    @Test
    void publish_nao_deve_lancar_excecao_em_valores_limite() {
        var publisher = new InMemoryEventPublisher();
        var dto = new InventoryDTO("SKU-0", 0, 0, 0L, Instant.EPOCH);

        assertDoesNotThrow(() ->
                publisher.publishInventoryUpdated(dto, "LOJA00", 0L)
        );
    }
}