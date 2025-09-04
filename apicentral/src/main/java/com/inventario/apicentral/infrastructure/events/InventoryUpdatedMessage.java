package com.inventario.apicentral.infrastructure.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InventoryUpdatedMessage(
        String eventId,
        String sku,
        Aggregate aggregate,
        String originStoreId,
        long originSeq,
        Instant updatedAt
) {
    public static record Aggregate(int onHand, int reserved, long version) {}

    public static InventoryUpdatedMessage of(
            String sku, int onHand, int reserved, long version,
            String originStoreId, long originSeq, Instant updatedAt
    ) {
        return new InventoryUpdatedMessage(
                UUID.randomUUID().toString(),
                sku,
                new Aggregate(onHand, reserved, version),
                originStoreId,
                originSeq,
                updatedAt
        );
    }
}