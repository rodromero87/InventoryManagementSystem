package com.inventario.loja.application;

import com.inventario.loja.domain.port.out.CentralInventoryClient;
import com.inventario.loja.infrastructure.persistence.JpaLocalInventoryEntity;
import com.inventario.loja.infrastructure.persistence.SpringDataLocalInventoryRepository;
import com.inventario.loja.shared.dto.InventoryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GetInventoryQueryImplTest {

    @Mock
    CentralInventoryClient central;

    @Mock
    SpringDataLocalInventoryRepository localRepo;

    @InjectMocks
    GetInventoryQueryImpl sut;

    @Test
    @DisplayName("getBySku → prioriza Central e retorna InventoryView com source='CENTRAL'")
    void shouldReturnFromCentralWhenOk() {
        // given
        var instant = Instant.parse("2025-09-01T12:34:56Z");

        var remote = new InventoryView("ABC-123", 10, 2, 7L, instant, "IGNORED");
        given(central.getInventory("ABC-123")).willReturn(remote);

        // when
        InventoryView view = sut.getBySku("ABC-123");

        // then
        assertThat(view).isNotNull();
        assertThat(view.sku()).isEqualTo("ABC-123");
        assertThat(view.onHand()).isEqualTo(10);
        assertThat(view.reserved()).isEqualTo(2);
        assertThat(view.version()).isEqualTo(7L);
        assertThat(view.updatedAt()).isEqualTo(instant);
        assertThat(view.source()).isEqualTo("CENTRAL");
    }

    @Test
    @DisplayName("fallbackFromLocal → quando encontra no local, mapeia e source='LOCAL'")
    void fallbackFromLocal_entityFound() throws Exception {
        // given
        var entity = new JpaLocalInventoryEntity();
        entity.setSku("SKU-2");
        entity.setOnHand(5);
        entity.setReserved(1);
        entity.setSourceVersion(9L);
        entity.setUpdatedAt(Instant.parse("2025-09-01T10:00:00Z"));

        given(localRepo.findById("SKU-2")).willReturn(Optional.of(entity));

        // when (invoca método privado via reflection)
        InventoryView view = invokeFallbackFromLocal(sut, "SKU-2", new RuntimeException("central down"));

        // then
        assertThat(view).isNotNull();
        assertThat(view.sku()).isEqualTo("SKU-2");
        assertThat(view.onHand()).isEqualTo(5);
        assertThat(view.reserved()).isEqualTo(1);
        assertThat(view.version()).isEqualTo(9L);
        assertThat(view.updatedAt()).isEqualTo(Instant.parse("2025-09-01T10:00:00Z"));
        assertThat(view.source()).isEqualTo("LOCAL");
    }

    @Test
    @DisplayName("fallbackFromLocal → quando não encontra no local, retorna zeros, EPOCH e source='LOCAL-NOT-FOUND'")
    void fallbackFromLocal_notFound() throws Exception {
        // given
        given(localRepo.findById("NONE")).willReturn(Optional.empty());

        // when
        InventoryView view = invokeFallbackFromLocal(sut, "NONE", new RuntimeException("central down"));

        // then
        assertThat(view).isNotNull();
        assertThat(view.sku()).isEqualTo("NONE");
        assertThat(view.onHand()).isZero();
        assertThat(view.reserved()).isZero();
        assertThat(view.version()).isZero();
        assertThat(view.updatedAt()).isEqualTo(Instant.EPOCH);
        assertThat(view.source()).isEqualTo("LOCAL-NOT-FOUND");
    }

    private static InventoryView invokeFallbackFromLocal(GetInventoryQueryImpl target, String sku, Throwable cause) throws Exception {
        Method m = GetInventoryQueryImpl.class.getDeclaredMethod("fallbackFromLocal", String.class, Throwable.class);
        m.setAccessible(true);
        return (InventoryView) m.invoke(target, sku, cause);
    }
}
