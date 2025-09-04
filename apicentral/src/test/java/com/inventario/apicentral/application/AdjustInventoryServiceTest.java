package com.inventario.apicentral.application;

import com.inventario.apicentral.domain.model.Inventory;
import com.inventario.apicentral.domain.port.out.EventPublisher;
import com.inventario.apicentral.domain.port.out.IdempotencyStore;
import com.inventario.apicentral.domain.port.out.InventoryRepository;
import com.inventario.apicentral.shared.dto.AdjustCommand;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustInventoryServiceTest {

    @Mock
    InventoryRepository repo;
    @Mock
    IdempotencyStore idempotency;
    @Mock
    EventPublisher publisher;

    @InjectMocks
    AdjustInventoryService service;

    private static final String SKU = "ABC-123";
    private static final String STORE = "LOJA01";

    private AdjustCommand cmd(int delta, long seq, String key) {
        return new AdjustCommand(STORE, SKU, delta, key, seq, Instant.parse("2025-09-01T00:00:00Z"));
    }


    @Test
    void ajusta_com_sucesso_salva_publica_e_marca_idempotencia() {
        // dado um estado atual onHand=10, reserved=2, version=5
        var now = Instant.parse("2025-09-01T10:00:00Z");
        var current = new Inventory(SKU, 10, 2, 5, now);
        when(idempotency.exists("K1")).thenReturn(false);
        when(repo.findBySkuForUpdate(SKU)).thenReturn(Optional.of(current));

        // o upsert devolve versão incrementada (simulando @Version do JPA)
        var saved = new Inventory(SKU, 15, 2, 6, now); // delta=+5
        when(repo.upsert(any())).thenReturn(saved);

        var result = service.adjust(cmd(+5, 1, "K1"));

        assertThat(result).isNotNull();
        assertThat(result.sku()).isEqualTo(SKU);
        assertThat(result.onHand()).isEqualTo(15);
        assertThat(result.reserved()).isEqualTo(2);
        assertThat(result.version()).isEqualTo(6);

        // idempotenty marcado DEPOIS do persist
        InOrder io = inOrder(repo, idempotency, publisher);
        io.verify(repo).findBySkuForUpdate(SKU);
        io.verify(repo).upsert(any());
        io.verify(idempotency).save("K1");
        io.verify(publisher).publishInventoryUpdated(any(InventoryDTO.class), eq(STORE), eq(1L));

        // nenhum outro chamado
        verifyNoMoreInteractions(repo, idempotency, publisher);
    }

    @Test
    void rejeita_quando_atp_ficaria_negativo() {
        // onHand=2, reserved=1 => ATP=1; delta=-5 -> ATP novo = -4
        var current = new Inventory(SKU, 2, 1, 0, Instant.parse("2025-09-01T00:00:00Z"));
        when(idempotency.exists("K_NEG")).thenReturn(false);
        when(repo.findBySkuForUpdate(SKU)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.adjust(cmd(-5, 10, "K_NEG")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ATP").hasMessageContaining("negativ");

        // não salva nem publica nem marca idempotência
        verify(repo, never()).upsert(any());
        verify(publisher, never()).publishInventoryUpdated(any(), anyString(), anyLong());
        verify(idempotency, never()).save(anyString());
    }

    @Test
    void idempotencia_existente_na_primeira_linha_nao_persistir_nem_publicar() {
        when(idempotency.exists("K_DUP")).thenReturn(true);
        // quando idempotência existe, o service retorna estado atual se houver…
        when(repo.findBySku(SKU)).thenReturn(Optional.of(new Inventory(SKU, 7, 0, 3, Instant.EPOCH)));

        var out = service.adjust(cmd(+1, 99, "K_DUP"));
        assertThat(out.onHand()).isEqualTo(7);
        assertThat(out.version()).isEqualTo(3);

        verify(repo, never()).findBySkuForUpdate(anyString());
        verify(repo, never()).upsert(any());
        verify(publisher, never()).publishInventoryUpdated(any(), anyString(), anyLong());
        verify(idempotency, never()).save(anyString());
    }

    @Test
    void cria_sku_quando_nao_existe_ainda() {
        when(idempotency.exists("K_NEW")).thenReturn(false);
        when(repo.findBySkuForUpdate(SKU)).thenReturn(Optional.empty());

        // se não existe, o service começa de onHand=0 => com delta +8, novo onHand=8
        var saved = new Inventory(SKU, 8, 0, 1, Instant.parse("2025-09-01T12:00:00Z"));
        when(repo.upsert(any())).thenReturn(saved);

        var out = service.adjust(cmd(+8, 1, "K_NEW"));

        assertThat(out.onHand()).isEqualTo(8);
        assertThat(out.version()).isEqualTo(1);

        verify(publisher).publishInventoryUpdated(any(InventoryDTO.class), eq(STORE), eq(1L));
        verify(idempotency).save("K_NEW");
    }

    @Test
    void idempotencia_existente_sem_registro_do_sku_retorna_default_zero() {
        when(idempotency.exists("K_MISS")).thenReturn(true);
        when(repo.findBySku(SKU)).thenReturn(Optional.empty());

        var out = service.adjust(cmd(+1, 1, "K_MISS"));
        // o service retorna um DTO default (sku, zeros, updatedAt=EPOCH) quando não achar
        assertThat(out.sku()).isEqualTo(SKU);
        assertThat(out.onHand()).isZero();
        assertThat(out.reserved()).isZero();
        assertThat(out.version()).isZero();

        verifyNoInteractions(publisher);
        verify(repo, never()).upsert(any());
        verify(idempotency, never()).save(anyString());
    }
}