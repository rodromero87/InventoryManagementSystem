package com.inventario.loja.application;

import com.inventario.loja.domain.port.out.CentralInventoryClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.doThrow;

@ExtendWith(MockitoExtension.class)
class LocalAdjustServiceTest {

    @Mock
    CentralInventoryClient central;

    @InjectMocks
    LocalAdjustService sut;

    @Test
    @DisplayName("adjustLocalAndPropagate compõe idempotencyKey e chama central.adjust com timestamp atual")
    void adjustLocalAndPropagate_callsCentralWithComposedKeyAndNow() {
        // given
        String storeId = "STORE-1";
        String sku = "ABC-123";
        int delta = 5;
        long storeSeq = 17L;

        Instant before = Instant.now();

        // when
        sut.adjustLocalAndPropagate(storeId, sku, delta, storeSeq);

        Instant after = Instant.now();

        // then: captura dos argumentos
        ArgumentCaptor<String> idempotencyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> instantCap = ArgumentCaptor.forClass(Instant.class);

        verify(central).adjust(
                eq(storeId),
                eq(sku),
                eq(delta),
                idempotencyCap.capture(),
                eq(storeSeq),
                instantCap.capture()
        );

        // idempotencyKey correta
        assertThat(idempotencyCap.getValue()).isEqualTo("STORE-1-ABC-123-17");

        // timestamp dentro da janela (±1s de margem)
        Instant sentAt = instantCap.getValue();
        assertThat(sentAt).isAfterOrEqualTo(before.minusSeconds(1));
        assertThat(sentAt).isBeforeOrEqualTo(after.plusSeconds(1));
    }

    @Test
    @DisplayName("adjustLocalAndPropagate propaga exceções do CentralInventoryClient")
    void adjustLocalAndPropagate_propagatesException() {
        String storeId = "S1";
        String sku = "X";
        int delta = -3;
        long storeSeq = 99L;

        doThrow(new RuntimeException("central down")).when(central)
                .adjust(eq(storeId), eq(sku), eq(delta), anyString(), eq(storeSeq), any(Instant.class));

        assertThatThrownBy(() -> sut.adjustLocalAndPropagate(storeId, sku, delta, storeSeq))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("central down");
    }
}
