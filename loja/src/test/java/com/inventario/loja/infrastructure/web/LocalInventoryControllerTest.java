package com.inventario.loja.infrastructure.web;

import com.inventario.loja.application.LocalAdjustService;
import com.inventario.loja.infrastructure.web.dto.LocalAdjustRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LocalInventoryControllerTest {

    @Mock
    LocalAdjustService service;

    @InjectMocks
    LocalInventoryController controller;

    @Test
    @DisplayName("POST /local/adjust (JSON) → chama o service e retorna 202 Accepted")
    void adjust_withBody_callsServiceAndReturnsAccepted() {
        // given
        var req = new LocalAdjustRequest("STORE-1", "ABC-123", 5, 17L, null);

        // when
        ResponseEntity<Void> resp = controller.adjust(req);

        // then
        then(service).should(times(1))
                .adjustLocalAndPropagate("STORE-1", "ABC-123", 5, 17L);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCode().value()).isEqualTo(202);
        assertThat(resp.getBody()).isNull();
    }

    @Test
    @DisplayName("Propaga exatamente os campos do body (inclui delta negativo)")
    void adjust_withBody_propagatesExactFields_evenNegativeDelta() {
        // given
        var req = new LocalAdjustRequest("S2", "SKU-NEG", -3, 99L, null);

        // when
        ResponseEntity<Void> resp = controller.adjust(req);

        // then
        then(service).should().adjustLocalAndPropagate("S2", "SKU-NEG", -3, 99L);
        assertThat(resp.getStatusCode().value()).isEqualTo(202);
        assertThat(resp.getBody()).isNull();
    }

}
