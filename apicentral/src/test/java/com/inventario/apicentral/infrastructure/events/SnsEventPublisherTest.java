package com.inventario.apicentral.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnsEventPublisherTest {

    @Mock
    SnsAsyncClient sns;
    ObjectMapper om;
    SnsEventPublisher publisher;

    final String TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:inventory-updates";

    @BeforeEach
    void setUp() {
        om = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        publisher = new SnsEventPublisher(sns, TOPIC_ARN, om);
    }

    @Test
    void devePublicarNoTopicoComJsonValido() throws Exception {
        // arrange
        var dto = new InventoryDTO("ABC-123", 50, 2, 5L, Instant.parse("2025-09-01T12:00:00Z"));
        when(sns.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder()
                        .messageId("mid-1").build()));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);

        // act
        publisher.publishInventoryUpdated(dto, "LOJA01", 42L);

        // assert
        verify(sns, times(1)).publish(captor.capture());
        PublishRequest req = captor.getValue();

        assertThat(req.topicArn()).isEqualTo(TOPIC_ARN);
        assertThat(req.message()).isNotBlank();

        JsonNode root = om.readTree(req.message());

        // eventId é gerado — apenas ver se existe
        assertThat(root.hasNonNull("eventId")).isTrue();
        assertThat(root.path("sku").asText()).isEqualTo("ABC-123");
        assertThat(root.path("originStoreId").asText()).isEqualTo("LOJA01");
        assertThat(root.path("originSeq").asLong()).isEqualTo(42L);
        assertThat(root.path("updatedAt").asText()).isEqualTo("2025-09-01T12:00:00Z");

        // os campos do agregado podem vir aninhados em "aggregate" ou na raiz, cobrimos ambos:
        int onHand = root.has("aggregate") ? root.path("aggregate").path("onHand").asInt()
                : root.path("onHand").asInt();
        int reserved = root.has("aggregate") ? root.path("aggregate").path("reserved").asInt()
                : root.path("reserved").asInt();
        long version = root.has("aggregate") ? root.path("aggregate").path("version").asLong()
                : root.path("version").asLong();

        assertThat(onHand).isEqualTo(50);
        assertThat(reserved).isEqualTo(2);
        assertThat(version).isEqualTo(5L);
    }

    @Test
    void devePropagarErroQuandoSnsFalhar() {
        var dto = new InventoryDTO("ABC-123", 10, 0, 1L, Instant.parse("2025-09-01T00:00:00Z"));
        // força falha imediata na chamada (não apenas na Future)
        when(sns.publish(any(PublishRequest.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () ->
                publisher.publishInventoryUpdated(dto, "LOJA01", 1L)
        );
    }
}