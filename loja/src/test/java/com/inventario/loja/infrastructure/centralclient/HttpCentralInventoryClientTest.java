package com.inventario.loja.infrastructure.centralclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventario.loja.shared.dto.InventoryView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpCentralInventoryClientTest {

    @Test
    void adjust_postsJsonCorrect() {

        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(0, new MappingJackson2HttpMessageConverter(om));
                });

        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();

        HttpCentralInventoryClient client = new HttpCentralInventoryClient(rest);

        var storeId = "STORE-1";
        var sku = "ABC-123";
        int delta = 5;
        long storeSeq = 17L;
        var key = "STORE-1-ABC-123-17";
        var occurredAt = java.time.Instant.parse("2025-09-01T12:34:56Z");

        server.expect(requestTo("http://localhost/inventory/adjust"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.delta").value(delta))
                .andExpect(jsonPath("$.idempotencyKey").value(key))
                .andExpect(jsonPath("$.storeSeq").value((int) storeSeq))
                .andExpect(jsonPath("$.occurredAt").value(occurredAt.toString()))
                .andRespond(withSuccess());

        client.adjust(storeId, sku, delta, key, storeSeq, occurredAt);

        server.verify();
    }

    @Test
    void getInventory_getsAndDeserializes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();

        HttpCentralInventoryClient client = new HttpCentralInventoryClient(rest);

        String sku = "ABC-123";
        String respJson = """
        {
          "sku": "ABC-123",
          "onHand": 10,
          "reserved": 2,
          "version": 7,
          "updatedAt": "2025-09-01T12:34:56Z",
          "source": "CENTRAL"
        }
        """;

        server.expect(requestTo("http://localhost/inventory/ABC-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(respJson, MediaType.APPLICATION_JSON));

        InventoryView view = client.getInventory(sku);

        assertThat(view).isNotNull();
        assertThat(view.sku()).isEqualTo("ABC-123");
        assertThat(view.onHand()).isEqualTo(10);
        assertThat(view.reserved()).isEqualTo(2);
        assertThat(view.version()).isEqualTo(7L);
        assertThat(view.updatedAt()).isEqualTo(Instant.parse("2025-09-01T12:34:56Z"));
        assertThat(view.source()).isEqualTo("CENTRAL");

        server.verify();
    }
}