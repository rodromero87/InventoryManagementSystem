package com.inventario.loja.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InventoryView(
        String sku,
        int onHand,
        int reserved,
        long version,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant updatedAt,
        String source
) {}
