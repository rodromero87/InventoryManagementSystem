package com.inventario.loja.infrastructure.web;

import com.inventario.loja.application.LocalAdjustService;
import com.inventario.loja.infrastructure.web.dto.LocalAdjustRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/local")
public class LocalInventoryController {

    private final LocalAdjustService service;

    public LocalInventoryController(LocalAdjustService service) {
        this.service = service;
    }

    @PostMapping("/adjust")
    public ResponseEntity<Void> adjust(@Valid @RequestBody LocalAdjustRequest req) {
        log.info("Disparando ajuste para a Central: storeId={}, sku={}, delta={}, seq={}",
                req.storeId(), req.sku(), req.delta(), req.seq());

        service.adjustLocalAndPropagate(
                req.storeId(),
                req.sku(),
                req.delta(),
                req.seq()
        );
        return ResponseEntity.accepted().build();
    }
}
