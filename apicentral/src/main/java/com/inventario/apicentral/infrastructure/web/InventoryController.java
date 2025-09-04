package com.inventario.apicentral.infrastructure.web;

import com.inventario.apicentral.domain.port.in.AdjustInventoryUseCase;
import com.inventario.apicentral.domain.port.in.GetInventoryQuery;
import com.inventario.apicentral.shared.dto.AdjustCommand;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final AdjustInventoryUseCase adjust;
    private final GetInventoryQuery get;

    public InventoryController(AdjustInventoryUseCase adjust, GetInventoryQuery get) {
        this.adjust = adjust; this.get = get;
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryDTO> adjust(@RequestBody AdjustCommand c){
        log.info("Central recebendo ajusto de Inventory");
        return ResponseEntity.ok(adjust.adjust(c));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryDTO> getBySku(@PathVariable String sku){
        return ResponseEntity.ok(get.getBySku(sku));
    }
}
