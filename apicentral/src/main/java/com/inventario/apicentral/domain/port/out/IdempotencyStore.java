package com.inventario.apicentral.domain.port.out;

public interface IdempotencyStore {
    boolean exists(String id);
    void save(String id);
}
