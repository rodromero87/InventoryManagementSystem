package com.inventario.loja.application;

import com.inventario.loja.domain.port.out.CentralInventoryClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LocalAdjustService {

    private final CentralInventoryClient central;

    public LocalAdjustService(CentralInventoryClient central) {
        this.central = central;
    }

    /**
     * Dispara um ajuste local para a Central.
     * A Loja só aplica quando receber o evento de volta via SQS.
     */
    public void adjustLocalAndPropagate(String storeId, String sku, int delta, long storeSeq) {
        String idempotencyKey = storeId + "-" + sku + "-" + storeSeq;
        central.adjust(storeId, sku, delta, idempotencyKey, storeSeq, Instant.now());
    }
}
