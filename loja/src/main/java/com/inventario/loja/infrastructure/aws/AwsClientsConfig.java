package com.inventario.loja.infrastructure.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;
import java.util.Optional;

@Configuration
public class AwsClientsConfig {

    private final Region region;
    private final Optional<URI> endpointOpt;

    public AwsClientsConfig(@Value("${aws.region}") String region,
                            @Value("${aws.endpoint:}") String endpoint) {
        this.region = Region.of(region);
        this.endpointOpt = (endpoint == null || endpoint.isBlank())
                ? Optional.empty()
                : Optional.of(URI.create(endpoint));
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        boolean local = endpointOpt.isPresent();
        AwsCredentialsProvider creds = local
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                : DefaultCredentialsProvider.create();

        SqsAsyncClientBuilder b = SqsAsyncClient.builder()
                .region(region)
                .credentialsProvider(creds);
        endpointOpt.ifPresent(b::endpointOverride);
        return b.build();
    }
}
