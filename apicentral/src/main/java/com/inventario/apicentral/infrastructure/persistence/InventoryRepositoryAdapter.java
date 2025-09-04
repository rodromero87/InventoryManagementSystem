package com.inventario.apicentral.infrastructure.persistence;

import com.inventario.apicentral.domain.model.Inventory;
import com.inventario.apicentral.domain.port.out.InventoryRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final SpringDataInventoryRepository jpa;

    public InventoryRepositoryAdapter(SpringDataInventoryRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Inventory> findBySku(String sku) {
        return jpa.findById(sku).map(this::toDomain);
    }

    @Override
    public Optional<Inventory> findBySkuForUpdate(String sku) {
        return jpa.findBySkuForUpdate(sku).map(this::toDomain);
    }

    @Override
    public Inventory upsert(Inventory inv) {

        var entity = jpa.findById(inv.sku()).orElseGet(() -> {
            var e = new JpaInventoryEntity();
            e.setSku(inv.sku());
            e.setOnHand(0);
            e.setReserved(0);
            e.setVersion(inv.version());
            e.setUpdatedAt(inv.updatedAt());
            return e;
        });

        entity.setOnHand(inv.onHand());
        entity.setReserved(inv.reserved());
        entity.setUpdatedAt(inv.updatedAt());

        var saved = jpa.saveAndFlush(entity);
        return toDomain(saved);
    }

    private Inventory toDomain(JpaInventoryEntity e) {
        return new Inventory(
                e.getSku(),
                e.getOnHand(),
                e.getReserved(),
                e.getVersion(),
                e.getUpdatedAt()
        );
    }
}