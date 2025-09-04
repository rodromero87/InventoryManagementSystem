package com.inventario.loja.infrastructure.persistence;

import com.inventario.loja.domain.port.out.AppliedEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AppliedEventRepositoryAdapter implements AppliedEventRepository {

    private final SpringDataAppliedEventRepository repo;

    public AppliedEventRepositoryAdapter(SpringDataAppliedEventRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean seen(String eventId) {
        return repo.existsById(eventId);
    }

    @Override
    public void mark(String eventId) {
        repo.saveAndFlush(new JpaAppliedEventEntity(eventId, Instant.now()));
    }
}