package com.inventario.apicentral.infrastructure.web;

import com.inventario.apicentral.domain.port.in.AdjustInventoryUseCase;
import com.inventario.apicentral.domain.port.in.GetInventoryQuery;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class InventoryControllerUnitTest {

    @Mock
    GetInventoryQuery getInventoryQuery;

    @Mock
    AdjustInventoryUseCase adjustInventoryUseCase;

    @InjectMocks
    InventoryController controller;

    @Test
    @DisplayName("getBySku deve retornar o DTO esperado")
    void getBySku_ok() {
        var expected = new InventoryDTO("ABC-123", 10, 2, 7, Instant.parse("2024-01-10T12:34:56Z"));
        given(getInventoryQuery.getBySku("ABC-123")).willReturn(expected);

        Object result = controller.getBySku("ABC-123"); // <-- troque o nome se o seu método for outro
        InventoryDTO body = unwrap(result, InventoryDTO.class);

        assertThat(body).isNotNull();
        assertThat(body.sku()).isEqualTo("ABC-123");
        assertThat(body.onHand()).isEqualTo(10);
        assertThat(body.reserved()).isEqualTo(2);
        assertThat(body.version()).isEqualTo(7);
        assertThat(body.updatedAt()).isEqualTo(Instant.parse("2024-01-10T12:34:56Z"));
    }

    @Test
    @DisplayName("getBySku deve retornar DTO default quando não existir")
    void getBySku_notFound_returnsDefault() {

        var expected = new InventoryDTO("NOT-FOUND", 0, 0, 0, Instant.EPOCH);
        given(getInventoryQuery.getBySku("NOT-FOUND")).willReturn(expected);

        Object result = controller.getBySku("NOT-FOUND");
        InventoryDTO body = unwrap(result, InventoryDTO.class);

        assertThat(body).isNotNull();
        assertThat(body.sku()).isEqualTo("NOT-FOUND");
        assertThat(body.onHand()).isZero();
        assertThat(body.reserved()).isZero();
        assertThat(body.version()).isZero();
        assertThat(body.updatedAt()).isEqualTo(Instant.EPOCH);
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap(Object result, Class<T> type) {
        if (result == null) return null;
        if (type.isInstance(result)) return (T) result;
        if (result instanceof ResponseEntity<?> resp) {
            Object body = resp.getBody();
            if (body == null) return null;
            if (!type.isInstance(body)) {
                throw new IllegalStateException("Body não é do tipo esperado: " + body.getClass());
            }
            return (T) body;
        }
        throw new IllegalStateException("Retorno não suportado: " + result.getClass());
    }
}