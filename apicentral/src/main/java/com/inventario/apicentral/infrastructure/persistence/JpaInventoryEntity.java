package com.inventario.apicentral.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "inventory")
public class JpaInventoryEntity {

    @Id
    @Column(name = "sku", nullable = false, updatable = false, length = 120)
    private String sku;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JpaInventoryEntity() {}

    public JpaInventoryEntity(String sku, int onHand, int reserved, long version, Instant updatedAt) {
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = reserved;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getOnHand() { return onHand; }
    public void setOnHand(int onHand) { this.onHand = onHand; }

    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}