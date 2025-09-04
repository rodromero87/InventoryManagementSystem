package com.inventario.loja.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record LocalAdjustRequest(
        @NotBlank String storeId,
        @NotBlank String sku,
        @NotNull @Min(-1_000_000) @Max(1_000_000) Integer delta,
        @NotNull @Positive Long seq,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {}
