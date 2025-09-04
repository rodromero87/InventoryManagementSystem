package com.inventario.loja.infrastructure.web;

import com.inventario.loja.domain.port.in.GetInventoryQuery;
import com.inventario.loja.shared.dto.InventoryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ReadInventoryControllerTest {

    @Mock
    GetInventoryQuery useCase;

    @InjectMocks
    ReadInventoryController controller;

    @Test
    @DisplayName("GET /inventory/{sku} → retorna 200 OK com InventoryView")
    void get_returnsOkWithBody() {
        // given
        String sku = "ABC-123";
        InventoryView expected = new InventoryView(
                sku,
                10,
                2,
                7L,
                Instant.parse("2025-09-01T12:34:56Z"),
                "CENTRAL"
        );
        given(useCase.getBySku(sku)).willReturn(expected);

        // when
        ResponseEntity<InventoryView> resp = controller.get(sku);

        // then
        then(useCase).should(times(1)).getBySku(sku);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(expected);
        // sanity checks nos campos principais
        assertThat(resp.getBody().sku()).isEqualTo("ABC-123");
        assertThat(resp.getBody().onHand()).isEqualTo(10);
        assertThat(resp.getBody().version()).isEqualTo(7L);
    }

    @Test
    @DisplayName("GET /inventory/{sku} → se use case retorna null, responde 200 OK com body null")
    void get_returnsOkWithNullBodyWhenUseCaseReturnsNull() {
        // given
        String sku = "NONE";
        given(useCase.getBySku(sku)).willReturn(null);

        // when
        ResponseEntity<InventoryView> resp = controller.get(sku);

        // then
        then(useCase).should(times(1)).getBySku(sku);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isNull();
    }
}
