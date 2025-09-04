package com.inventario.apicentral.infrastructure.persistence;

import com.inventario.apicentral.domain.model.Inventory;
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
class InventoryRepositoryAdapterTest {

    @Mock
    SpringDataInventoryRepository jpa;

    @InjectMocks
    InventoryRepositoryAdapter adapter;

    private static JpaInventoryEntity jpaEntity(String sku, int onHand, int reserved, long version, Instant ts) {
        var e = new JpaInventoryEntity();
        e.setSku(sku);
        e.setOnHand(onHand);
        e.setReserved(reserved);
        e.setVersion(version);
        e.setUpdatedAt(ts);
        return e;
    }

    @Test
    void findBySkuForUpdate_deve_mapear_da_entidade_para_dominio() {
        var ts = Instant.parse("2025-09-01T12:00:00Z");
        when(jpa.findBySkuForUpdate("ABC-123"))
                .thenReturn(Optional.of(jpaEntity("ABC-123", 10, 2, 5L, ts)));

        var out = adapter.findBySkuForUpdate("ABC-123");

        assertThat(out).isPresent();
        Inventory inv = out.get();
        assertThat(inv.sku()).isEqualTo("ABC-123");
        assertThat(inv.onHand()).isEqualTo(10);
        assertThat(inv.reserved()).isEqualTo(2);
        assertThat(inv.version()).isEqualTo(5L);
        assertThat(inv.updatedAt()).isEqualTo(ts);

        verify(jpa, times(1)).findBySkuForUpdate("ABC-123");
        verifyNoMoreInteractions(jpa);
    }

}