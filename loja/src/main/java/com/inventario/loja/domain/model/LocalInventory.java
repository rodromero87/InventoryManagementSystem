package com.inventario.loja.domain.model;

import java.time.Instant;
import java.util.Objects;

public record LocalInventory(
        String sku,
        int onHand,
        int reserved,
        long sourceVersion,  // versão do estado vindo da Central
        Instant updatedAt
) {
    public LocalInventory {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (onHand < 0)        throw new IllegalArgumentException("onHand não pode ser negativo");
        if (reserved < 0)      throw new IllegalArgumentException("reserved não pode ser negativo");
        if (sourceVersion < 0) throw new IllegalArgumentException("sourceVersion não pode ser negativo");
    }

    /** Available-To-Promise = onHand - reserved */
    public int atp() { return onHand - reserved; }

    /** Retorna uma cópia com novo onHand (não altera versão). */
    public LocalInventory withOnHand(int newOnHand, Instant when) {
        if (newOnHand < 0) throw new IllegalArgumentException("onHand não pode ser negativo");
        return new LocalInventory(sku, newOnHand, reserved, sourceVersion, when);
    }

    /** Retorna uma cópia com novo reserved (não altera versão). */
    public LocalInventory withReserved(int newReserved, Instant when) {
        if (newReserved < 0) throw new IllegalArgumentException("reserved não pode ser negativo");
        return new LocalInventory(sku, onHand, newReserved, sourceVersion, when);
    }

    /** Atualiza apenas a versão (útil após persistir/receber evento). */
    public LocalInventory withSourceVersion(long newVersion, Instant when) {
        if (newVersion < 0) throw new IllegalArgumentException("sourceVersion não pode ser negativo");
        return new LocalInventory(sku, onHand, reserved, newVersion, when);
    }

    /** Atualiza somente o timestamp. */
    public LocalInventory withUpdatedAt(Instant when) {
        return new LocalInventory(sku, onHand, reserved, sourceVersion, when);
    }

    /**
     * Aplica um estado completo (ex.: vindo de evento da Central).
     * Garante valores não negativos e troca atômica de (onHand, reserved, version, updatedAt).
     */
    public LocalInventory applyState(int newOnHand, int newReserved, long newVersion, Instant when) {
        if (newOnHand < 0 || newReserved < 0 || newVersion < 0) {
            throw new IllegalArgumentException("valores não podem ser negativos");
        }
        return new LocalInventory(sku, newOnHand, newReserved, newVersion, Objects.requireNonNull(when, "when"));
    }
}