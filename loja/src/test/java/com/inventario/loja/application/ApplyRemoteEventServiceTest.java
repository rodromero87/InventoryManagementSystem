package com.inventario.loja.application;

import com.inventario.loja.domain.port.out.AppliedEventRepository;
import com.inventario.loja.infrastructure.persistence.JpaLocalInventoryEntity;
import com.inventario.loja.infrastructure.persistence.LocalInventoryRepositoryAdapter;
import com.inventario.loja.shared.dto.InventoryUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ApplyRemoteEventServiceTest {

    @Mock
    LocalInventoryRepositoryAdapter repo;

    @Mock
    AppliedEventRepository applied;

    @InjectMocks
    ApplyRemoteEventService sut;

    @Test
    @DisplayName("Idempotência: se applied.seen(eventId)=true, não faz nada")
    void shouldNoopWhenEventAlreadySeen() {
        var e = evt("e-1", "SKU-1", 10, 2, 5, Instant.parse("2025-09-01T12:00:00Z"));
        given(applied.seen("e-1")).willReturn(true);

        sut.apply(e);

        then(repo).shouldHaveNoInteractions();
        then(applied).should(never()).mark(anyString());
    }

    @Test
    @DisplayName("Se versão local >= versão do evento, só marca e não salva")
    void shouldOnlyMarkWhenLocalVersionIsGreaterOrEqual() {
        var e = evt("e-2", "SKU-2", 11, 3, 7, Instant.parse("2025-09-01T13:00:00Z"));
        given(applied.seen("e-2")).willReturn(false);

        var existing = new JpaLocalInventoryEntity();
        existing.setSku("SKU-2");
        existing.setOnHand(5);
        existing.setReserved(1);
        existing.setSourceVersion(7); // >= e.version()
        existing.setUpdatedAt(Instant.parse("2025-08-31T10:00:00Z"));

        given(repo.findForUpdate("SKU-2")).willReturn(Optional.of(existing));

        sut.apply(e);

        then(repo).should(never()).save(any(JpaLocalInventoryEntity.class));
        then(applied).should().mark("e-2");
    }

    @Test
    @DisplayName("Se versão local < versão do evento, atualiza entidade e salva")
    void shouldUpdateAndSaveWhenIncomingVersionIsNewer() {
        var e = evt("e-3", "SKU-3", 20, 4, 9, Instant.parse("2025-09-01T14:00:00Z"));
        given(applied.seen("e-3")).willReturn(false);

        var existing = new JpaLocalInventoryEntity();
        existing.setSku("SKU-3");
        existing.setOnHand(7);
        existing.setReserved(2);
        existing.setSourceVersion(8); // < 9
        existing.setUpdatedAt(Instant.parse("2025-08-30T09:00:00Z"));

        given(repo.findForUpdate("SKU-3")).willReturn(Optional.of(existing));

        ArgumentCaptor<JpaLocalInventoryEntity> cap = ArgumentCaptor.forClass(JpaLocalInventoryEntity.class);

        sut.apply(e);

        then(repo).should().save(cap.capture());
        then(applied).should().mark("e-3");

        var saved = cap.getValue();
        assertThat(saved.getSku()).isEqualTo("SKU-3");
        assertThat(saved.getOnHand()).isEqualTo(20);
        assertThat(saved.getReserved()).isEqualTo(4);
        assertThat(saved.getSourceVersion()).isEqualTo(9);
        assertThat(saved.getUpdatedAt()).isEqualTo(Instant.parse("2025-09-01T14:00:00Z"));
    }

    @Test
    @DisplayName("Se não existe entidade, cria nova, popula pelos dados do evento e salva")
    void shouldCreateWhenNotFound() {
        var e = evt("e-4", "SKU-4", 5, 0, 1, Instant.parse("2025-09-01T15:00:00Z"));
        given(applied.seen("e-4")).willReturn(false);
        given(repo.findForUpdate("SKU-4")).willReturn(Optional.empty());

        ArgumentCaptor<JpaLocalInventoryEntity> cap = ArgumentCaptor.forClass(JpaLocalInventoryEntity.class);

        sut.apply(e);

        then(repo).should().save(cap.capture());
        then(applied).should().mark("e-4");

        var saved = cap.getValue();
        assertThat(saved.getSku()).isEqualTo("SKU-4");
        assertThat(saved.getOnHand()).isEqualTo(5);
        assertThat(saved.getReserved()).isZero();
        assertThat(saved.getSourceVersion()).isEqualTo(1);
        assertThat(saved.getUpdatedAt()).isEqualTo(Instant.parse("2025-09-01T15:00:00Z"));
    }

    private static InventoryUpdatedEvent evt(String id, String sku, int onHand, int reserved, int version, Instant updatedAt) {
        return new InventoryUpdatedEvent(id, sku, onHand, reserved, version, "store-1",  1L, updatedAt);
    }
}