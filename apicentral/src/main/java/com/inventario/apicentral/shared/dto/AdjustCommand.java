package com.inventario.apicentral.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AdjustCommand(@NotBlank String storeId,
                            @NotBlank String sku,
                            int delta,
                            @NotBlank String idempotencyKey,
                            @Positive long storeSeq,
                            @NotNull Instant occurredAt) {
}
