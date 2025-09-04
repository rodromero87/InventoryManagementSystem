package com.inventario.apicentral.application;

import com.inventario.apicentral.domain.model.Inventory;
import com.inventario.apicentral.domain.port.out.InventoryRepository;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetInventoryServiceTest {

    @Mock
    InventoryRepository repo;

    @InjectMocks
    GetInventoryService service;

    @Test
    void deve_mapear_quando_encontrar_sku() {
        // dado
        Instant now = Instant.parse("2025-09-01T10:00:00Z");
        var inv = new Inventory("ABC-123", 10, 2, 5, now);
        when(repo.findBySkuForUpdate("ABC-123")).thenReturn(Optional.of(inv));

        // quando
        InventoryDTO out = service.getBySku("ABC-123");

        // então
        assertThat(out.sku()).isEqualTo("ABC-123");
        assertThat(out.onHand()).isEqualTo(10);
        assertThat(out.reserved()).isEqualTo(2);
        assertThat(out.version()).isEqualTo(5);
        assertThat(out.updatedAt()).isEqualTo(now);

        verify(repo, times(1)).findBySkuForUpdate("ABC-123");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void deve_retornar_defaults_quando_nao_encontrar_sku() {
        when(repo.findBySkuForUpdate("MISSING")).thenReturn(Optional.empty());

        InventoryDTO out = service.getBySku("MISSING");

        assertThat(out.sku()).isEqualTo("MISSING");
        assertThat(out.onHand()).isZero();
        assertThat(out.reserved()).isZero();
        assertThat(out.version()).isZero();
        assertThat(out.updatedAt()).isEqualTo(Instant.EPOCH);

        verify(repo, times(1)).findBySkuForUpdate("MISSING");
        verifyNoMoreInteractions(repo);
    }
}