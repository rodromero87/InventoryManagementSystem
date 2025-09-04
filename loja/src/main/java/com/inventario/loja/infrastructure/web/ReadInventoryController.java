package com.inventario.loja.infrastructure.web;

import com.inventario.loja.domain.port.in.GetInventoryQuery;
import com.inventario.loja.shared.dto.InventoryView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/inventory")
public class ReadInventoryController {

    private final GetInventoryQuery useCase;

    public ReadInventoryController(GetInventoryQuery useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryView> get(@PathVariable String sku) {
        log.info("Consultando Invetory por SKU: ", ""+sku);
        return ResponseEntity.ok(useCase.getBySku(sku));
    }
}
