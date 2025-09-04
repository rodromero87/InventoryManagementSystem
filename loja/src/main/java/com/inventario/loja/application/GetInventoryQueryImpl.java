package com.inventario.loja.application;

import com.inventario.loja.domain.port.in.GetInventoryQuery;
import com.inventario.loja.domain.port.out.CentralInventoryClient;
import com.inventario.loja.infrastructure.persistence.JpaLocalInventoryEntity;
import com.inventario.loja.infrastructure.persistence.SpringDataLocalInventoryRepository;
import com.inventario.loja.shared.dto.InventoryView;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GetInventoryQueryImpl implements GetInventoryQuery {

    private final CentralInventoryClient central;
    private final SpringDataLocalInventoryRepository localRepo;

    public GetInventoryQueryImpl(CentralInventoryClient central,
                                 SpringDataLocalInventoryRepository localRepo) {
        this.central = central;
        this.localRepo = localRepo;
    }

    @Override
    @CircuitBreaker(name = "centralApi", fallbackMethod = "fallbackFromLocal")
    @Retry(name = "centralApi")
    public InventoryView getBySku(String sku) {
        var v = central.getInventory(sku);
        return new InventoryView(v.sku(), v.onHand(), v.reserved(), v.version(), v.updatedAt(), "CENTRAL");
    }

    private InventoryView fallbackFromLocal(String sku, Throwable cause) {
        return localRepo.findById(sku)
                .map(this::toView)
                .orElseGet(() -> new InventoryView(sku, 0, 0, 0, Instant.EPOCH, "LOCAL-NOT-FOUND"));
    }

    private InventoryView toView(JpaLocalInventoryEntity e) {
        return new InventoryView(
                e.getSku(), e.getOnHand(), e.getReserved(), e.getSourceVersion(), e.getUpdatedAt(), "LOCAL"
        );
    }
}