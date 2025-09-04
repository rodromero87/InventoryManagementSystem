package com.inventario.apicentral.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.apicentral.domain.port.out.EventPublisher;
import com.inventario.apicentral.shared.dto.InventoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "sns")
public class SnsEventPublisher implements EventPublisher {

    private final SnsAsyncClient sns;
    private final String topicArn;
    private final ObjectMapper om;

    public SnsEventPublisher(
            SnsAsyncClient sns,
            @Value("${aws.topic-arn}") String topicArn,
            ObjectMapper om
    ) {
        this.sns = sns;
        this.topicArn = topicArn;
        this.om = om;
    }

    @Override
    public void publishInventoryUpdated(InventoryDTO dto, String originStoreId, long originSeq) {
        try {
            var msg = InventoryUpdatedMessage.of(
                    dto.sku(), dto.onHand(), dto.reserved(), dto.version(),
                    originStoreId, originSeq, dto.updatedAt()
            );
            var json = om.writeValueAsString(msg);

            var req = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(json)
                    .build();

            sns.publish(req);
            log.info("SNS publish OK sku={} v={} origin={}#{}", dto.sku(), dto.version(), originStoreId, originSeq);
        } catch (Exception e) {
            log.error("SNS publish failed", e);
            throw new RuntimeException(e);
        }
    }
}