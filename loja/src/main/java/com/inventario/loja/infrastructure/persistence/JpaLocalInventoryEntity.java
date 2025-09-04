package com.inventario.loja.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory")
public class JpaLocalInventoryEntity {

    @Id
    @Column(length = 120, nullable = false, updatable = false)
    private String sku;

    @Column(nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    @Column(nullable = false)
    private long sourceVersion;

    @Column(nullable = false)
    private Instant updatedAt;

}
