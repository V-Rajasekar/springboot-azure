package com.raj.springboot.azure.cosmos.config;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.time.Duration;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.core.ResponseDiagnosticsProcessor;
import com.azure.spring.data.cosmos.core.convert.ObjectMapperFactory;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raj.springboot.azure.cosmos.exception.CosmosRetryException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCosmosRepositories(basePackages = {"com.raj.springboot.azure.cosmos.repository"})
public class CosmosDBConfiguration extends AbstractCosmosConfiguration {

    private final String uri;

    private final String key;

    private final String dbName;

    @Value("${configuration.max-retry}")
    private int maxAttempts;

    @Value("${configuration.initial-interval}")
    private int initialInterval;

    @Value("${spring.cosmos.max-degree-parallelism}")
    private int maxDegree;

    private final ResponseDiagnosticsProcessor responseDiagnosticsProcessor;

    AzureKeyCredential azureKeyCredential;

    @Autowired
    public CosmosDBConfiguration(@Value("${spring.cosmos.uri}") String uri,
                                 @Value("${spring.cosmos.key}") String key,
                                 @Value("${spring.cosmos.database}") String dbName,
                                 MeterRegistry meterRegistry,
                                 ResponseDiagnosticsProcessor responseDiagnosticsProcessor) {
        this.uri = uri;
        this.key = key;
        this.dbName = dbName;
        this.responseDiagnosticsProcessor = responseDiagnosticsProcessor;
    }

    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper om = ObjectMapperFactory.getObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL).disable(WRITE_DATES_AS_TIMESTAMPS);
        om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY).disable(WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    @Bean
    public CosmosClientBuilder clientBuilder() {
        this.azureKeyCredential = new AzureKeyCredential(this.key);
        DirectConnectionConfig directConnectionConfig = DirectConnectionConfig.getDefaultConfig();
        directConnectionConfig.setIdleEndpointTimeout(Duration.ofMinutes(30));

        return new CosmosClientBuilder()
                .endpoint(this.uri)
                .key(this.key)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .directMode(directConnectionConfig);
    }

    @Bean
    public CosmosConfig config() {
        return CosmosConfig.builder()
                .maxDegreeOfParallelism(maxDegree)
                .responseDiagnosticsProcessor(responseDiagnosticsProcessor)
                .enableQueryMetrics(true)
                .build();
    }

    @Bean(name = "retryConfig")
    public RetryConfig getRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(initialInterval, 2))
                .retryExceptions(CosmosRetryException.class)
                .ignoreExceptions(CosmosAccessException.class)
                .build();
    }

    @Override
    protected String getDatabaseName() {
        return dbName;
    }
}
