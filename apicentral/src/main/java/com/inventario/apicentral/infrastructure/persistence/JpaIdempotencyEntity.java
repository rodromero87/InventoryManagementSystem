package com.inventario.apicentral.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency")
public class JpaIdempotencyEntity {

    @Id
    @Column(name = "id", length = 200, nullable = false, updatable = false)
    private String id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaIdempotencyEntity() {}

    public JpaIdempotencyEntity(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
