package com.inventario.loja.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "applied_events")
public class JpaAppliedEventEntity {

    @Id
    @Column(length = 200, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private Instant createdAt;

    protected JpaAppliedEventEntity() {}

    public JpaAppliedEventEntity(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}