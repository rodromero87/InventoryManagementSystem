package com.inventario.loja.infrastructure.centralclient;

import com.inventario.loja.domain.port.out.CentralInventoryClient;
import com.inventario.loja.infrastructure.centralclient.dto.AdjustCommand;
import com.inventario.loja.shared.dto.InventoryView;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class HttpCentralInventoryClient implements CentralInventoryClient {

    private final RestClient rest;

    public HttpCentralInventoryClient(RestClient centralRestClient) {
        this.rest = centralRestClient;
    }

    @Override
    public void adjust(String storeId, String sku, int delta, String idempotencyKey, long storeSeq, Instant occurredAt) {
        var cmd = new AdjustCommand(storeId, sku, delta, idempotencyKey, storeSeq, occurredAt);
        rest.post()
                .uri("/inventory/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cmd)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public InventoryView getInventory(String sku) {
        return rest.get()
                .uri("/inventory/{sku}", sku)
                .retrieve()
                .body(InventoryView.class);
    }
}