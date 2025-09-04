package com.inventario.loja.infrastructure.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.loja.domain.port.in.ApplyRemoteEventUseCase;
import com.inventario.loja.shared.dto.InventoryUpdatedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.*;

@Component
public class SqsInventoryConsumer {
    private static final Logger log = LoggerFactory.getLogger(SqsInventoryConsumer.class);

    private final SqsAsyncClient sqs;
    private final ApplyRemoteEventUseCase apply;
    private final ObjectMapper om;
    private final String queueUrl;

    private ScheduledExecutorService exec;

    public SqsInventoryConsumer(
            SqsAsyncClient sqs,
            ApplyRemoteEventUseCase apply,
            ObjectMapper om,
            @Value("${aws.queue-url}") String queueUrl
    ) {
        this.sqs = sqs; this.apply = apply; this.om = om; this.queueUrl = queueUrl;
    }

    @PostConstruct
    void start() {
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "sqs-consumer"); t.setDaemon(true); return t;
        });
        exec.scheduleWithFixedDelay(this::pollOnce, 0, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() { if (exec != null) exec.shutdownNow(); }

    void pollOnce() {
        try {
            var req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(20)
                    .visibilityTimeout(30)
                    .maxNumberOfMessages(10)
                    .build();

            var resp = sqs.receiveMessage(req).get(30, TimeUnit.SECONDS);
            List<Message> messages = resp.messages();
            if (messages == null || messages.isEmpty()) return;

            log.info("Recebendo msg SQS");

            for (var m : messages) {
                try {
                    String body = m.body();

                    if (looksLikeSnsEnvelope(body)) {
                        var node = om.readTree(body);
                        body = node.get("Message").asText();
                    }
                    var evt = om.readValue(body, InventoryUpdatedEvent.class);
                    apply.apply(evt);

                    sqs.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(m.receiptHandle())
                            .build());
                } catch (Exception ex) {
                    log.error("Falha processando msg id={}", m.messageId(), ex);
                }
            }
        } catch (Exception e) {
            log.warn("Erro no long-polling SQS, tentando novamente", e);
        }
    }

    private boolean looksLikeSnsEnvelope(String body) {
        return body != null && body.contains("\"Type\"") && body.contains("\"Message\"");
    }
}

