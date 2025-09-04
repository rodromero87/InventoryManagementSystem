package com.inventario.loja.domain.port.out;

public interface AppliedEventRepository {
    boolean seen(String eventId);
    void mark(String eventId);
}
