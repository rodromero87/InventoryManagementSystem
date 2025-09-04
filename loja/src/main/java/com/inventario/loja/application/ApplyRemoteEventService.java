package com.inventario.loja.application;

import com.inventario.loja.domain.port.in.ApplyRemoteEventUseCase;
import com.inventario.loja.domain.port.out.AppliedEventRepository;
import com.inventario.loja.infrastructure.persistence.JpaLocalInventoryEntity;
import com.inventario.loja.infrastructure.persistence.LocalInventoryRepositoryAdapter;
import com.inventario.loja.shared.dto.InventoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class ApplyRemoteEventService implements ApplyRemoteEventUseCase {

    private final LocalInventoryRepositoryAdapter repo;
    private final AppliedEventRepository applied;

    public ApplyRemoteEventService(LocalInventoryRepositoryAdapter repo,
                                   AppliedEventRepository applied) {
        this.repo = repo;
        this.applied = applied;
    }

    @Override
    @Transactional
    public void apply(InventoryUpdatedEvent e) {

        if (applied.seen(e.eventId())) return;

        var currentOpt = repo.findForUpdate(e.sku());
        if (currentOpt.isPresent() && currentOpt.get().getSourceVersion() >= e.version()) {
            applied.mark(e.eventId());
            return;
        }

        JpaLocalInventoryEntity entity = currentOpt.orElseGet(() -> {
            var n = new JpaLocalInventoryEntity();
            n.setSku(e.sku());
            n.setOnHand(0);
            n.setReserved(0);
            n.setSourceVersion(0);
            n.setUpdatedAt(Instant.now());
            return n;
        });

        entity.setOnHand(e.onHand());
        entity.setReserved(e.reserved());
        entity.setSourceVersion(e.version());
        entity.setUpdatedAt(e.updatedAt());

        repo.save(entity);
        applied.mark(e.eventId());

        log.info("Applied event {} sku={} v={} origin={}#{}",
                e.eventId(), e.sku(), e.version(), e.originStoreId(), e.originSeq());
    }
}