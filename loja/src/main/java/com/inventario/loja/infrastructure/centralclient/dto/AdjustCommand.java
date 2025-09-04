package com.inventario.loja.infrastructure.centralclient.dto;

import java.time.Instant;

public record AdjustCommand(
        String storeId,
        String sku,
        int delta,
        String idempotencyKey,
        long storeSeq,
        Instant occurredAt
) {}
