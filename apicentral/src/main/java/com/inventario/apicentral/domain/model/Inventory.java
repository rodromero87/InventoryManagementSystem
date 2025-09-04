package com.inventario.apicentral.domain.model;

import java.time.Instant;
import java.util.Objects;


public record Inventory(
        String sku,
        int onHand,
        int reserved,
        long version,
        Instant updatedAt
) {
    public Inventory {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (onHand < 0)   throw new IllegalArgumentException("onHand não pode ser negativo");
        if (reserved < 0) throw new IllegalArgumentException("reserved não pode ser negativo");
        if (version < 0)  throw new IllegalArgumentException("version não pode ser negativo");
    }

    public int atp() { return onHand - reserved; }

    public Inventory withOnHand(int newOnHand, Instant when) {
        if (newOnHand < 0) throw new IllegalArgumentException("onHand não pode ser negativo");
        return new Inventory(sku, newOnHand, reserved, version, when);
    }

    public Inventory withReserved(int newReserved, Instant when) {
        if (newReserved < 0) throw new IllegalArgumentException("reserved não pode ser negativo");
        return new Inventory(sku, onHand, newReserved, version, when);
    }

    public Inventory withVersion(long newVersion, Instant when) {
        if (newVersion < 0) throw new IllegalArgumentException("version não pode ser negativo");
        return new Inventory(sku, onHand, reserved, newVersion, when);
    }
}