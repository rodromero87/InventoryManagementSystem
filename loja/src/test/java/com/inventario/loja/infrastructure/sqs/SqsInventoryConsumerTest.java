package com.inventario.loja.infrastructure.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventario.loja.domain.port.in.ApplyRemoteEventUseCase;
import com.inventario.loja.shared.dto.InventoryUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SqsInventoryConsumerTest {

    @org.mockito.Mock SqsAsyncClient sqs;
    @org.mockito.Mock ApplyRemoteEventUseCase apply;

    private static ObjectMapper om() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private SqsInventoryConsumer newConsumer() {
        return new SqsInventoryConsumer(sqs, apply, om(), "http://queue-url");
    }

    @Test
    @DisplayName("pollOnce: sem mensagens → não aplica nem deleta")
    void pollOnce_empty() {
        var resp = ReceiveMessageResponse.builder().messages(List.of()).build();
        given(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(resp));

        var consumer = newConsumer();
        consumer.pollOnce();

        then(apply).shouldHaveNoInteractions();
        then(sqs).should(never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("pollOnce: mensagem JSON crua → desserializa, aplica e deleta")
    void pollOnce_plainJson_ok() throws Exception {
        var evt = new InventoryUpdatedEvent(
                "e-1",
                "SKU-1",
                10,
                2,
                7L,
                "store-1",
                1L,
                Instant.parse("2025-09-01T12:00:00Z")
        );

        String body = om().writeValueAsString(evt);

        var m = Message.builder()
                .messageId("m1")
                .receiptHandle("r1")
                .body(body)
                .build();

        var resp = ReceiveMessageResponse.builder().messages(m).build();

        given(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(resp));
        given(sqs.deleteMessage(any(DeleteMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        var consumer = newConsumer();
        consumer.pollOnce();

        ArgumentCaptor<InventoryUpdatedEvent> evtCap = ArgumentCaptor.forClass(InventoryUpdatedEvent.class);
        then(apply).should().apply(evtCap.capture());
        assertThat(evtCap.getValue()).isEqualTo(evt);

        ArgumentCaptor<DeleteMessageRequest> delCap = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        then(sqs).should().deleteMessage(delCap.capture());
        DeleteMessageRequest req = delCap.getValue();
        assertThat(req.queueUrl()).isEqualTo("http://queue-url");
        assertThat(req.receiptHandle()).isEqualTo("r1");
    }

    @Test
    @DisplayName("pollOnce: envelope SNS (Message) → desembrulha, aplica e deleta")
    void pollOnce_snsEnvelope_ok() throws Exception {
        var evt = new InventoryUpdatedEvent(
                "e-2",
                "SKU-2",
                5,
                1,
                3L,
                "store-9",
                42L,
                Instant.parse("2025-09-01T13:00:00Z")
        );

        String inner = om().writeValueAsString(evt);

        String envelope = """
            {"Type":"Notification","Message":%s}
            """.formatted(quoteJson(inner));

        var m = Message.builder().messageId("m2").receiptHandle("r2").body(envelope).build();
        var resp = ReceiveMessageResponse.builder().messages(m).build();

        given(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(resp));
        given(sqs.deleteMessage(any(DeleteMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        var consumer = newConsumer();
        consumer.pollOnce();

        then(apply).should().apply(evt);

        ArgumentCaptor<DeleteMessageRequest> delCap = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        then(sqs).should().deleteMessage(delCap.capture());
        assertThat(delCap.getValue().receiptHandle()).isEqualTo("r2");
    }

    @Test
    @DisplayName("pollOnce: falha ao aplicar UMA mensagem → não deleta a falha; continua nas demais")
    void pollOnce_oneFails_otherSucceeds() throws Exception {
        var badEvt = new InventoryUpdatedEvent(
                "bad","SKU-BAD",2,0,1L,"s",2L, Instant.parse("2025-09-01T15:00:00Z"));
        var okEvt  = new InventoryUpdatedEvent(
                "ok","SKU-OK",1,0,1L,"s",1L,  Instant.parse("2025-09-01T14:00:00Z"));

        var mBad = Message.builder().messageId("mb").receiptHandle("rb")
                .body(om().writeValueAsString(badEvt)).build();
        var mOk  = Message.builder().messageId("mo").receiptHandle("ro")
                .body(om().writeValueAsString(okEvt)).build();

        var resp = ReceiveMessageResponse.builder().messages(mBad, mOk).build();

        given(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(resp));
        willThrow(new RuntimeException("boom")).given(apply).apply(badEvt);
        willDoNothing().given(apply).apply(okEvt);

        given(sqs.deleteMessage(any(DeleteMessageRequest.class)))
                .willReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        var consumer = newConsumer();
        consumer.pollOnce();

        then(apply).should().apply(badEvt);
        then(apply).should().apply(okEvt);

        // Apenas a bem-sucedida (ro) é deletada
        ArgumentCaptor<DeleteMessageRequest> delCap = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        then(sqs).should(times(1)).deleteMessage(delCap.capture());
        assertThat(delCap.getValue().receiptHandle()).isEqualTo("ro");
    }

    @Test
    @DisplayName("pollOnce: erro no receiveMessage → capturado e nenhum delete é chamado")
    void pollOnce_receiveError_isCaught() {
        given(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("sqs down")));

        var consumer = newConsumer();
        consumer.pollOnce();

        then(apply).shouldHaveNoInteractions();
        then(sqs).should(never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    private static String quoteJson(String rawJson) {
        return "\"" + rawJson.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
