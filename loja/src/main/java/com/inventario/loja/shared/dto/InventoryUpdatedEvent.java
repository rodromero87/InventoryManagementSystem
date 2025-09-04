package com.inventario.loja.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryUpdatedEvent(
        String eventId,
        String sku,
        int onHand,
        int reserved,
        long version,
        String originStoreId,
        long originSeq,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant updatedAt
) {}
