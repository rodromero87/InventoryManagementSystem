package com.inventario.apicentral.infrastructure.persistence;


import com.inventario.apicentral.domain.port.out.IdempotencyStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class IdempotencyStoreAdapter implements IdempotencyStore {

    private final SpringDataIdempotencyRepository repo;

    public IdempotencyStoreAdapter(SpringDataIdempotencyRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String id) {
        return repo.existsById(id);
    }

    @Override
    @Transactional
    public void save(String id) {
        try {
            repo.saveAndFlush(new JpaIdempotencyEntity(id, Instant.now()));
        } catch (DataIntegrityViolationException dup) {
            // Já existe (concorrência): ok — operação é idempotente.
        }
    }
}

