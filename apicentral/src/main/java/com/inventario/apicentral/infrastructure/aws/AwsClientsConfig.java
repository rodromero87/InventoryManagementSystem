package com.inventario.apicentral.infrastructure.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;

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
    public SnsAsyncClient snsAsyncClient() {
        boolean useEndpointOverride = endpointOpt.isPresent();

        AwsCredentialsProvider creds = useEndpointOverride
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                : DefaultCredentialsProvider.create();

        SnsAsyncClientBuilder builder = SnsAsyncClient.builder()
                .region(region)
                .credentialsProvider(creds);

        endpointOpt.ifPresent(builder::endpointOverride);

        return builder.build();
    }
}