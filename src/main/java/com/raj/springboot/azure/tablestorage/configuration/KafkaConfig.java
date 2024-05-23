package com.raj.springboot.azure.tablestorage.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

@EnableKafka
@Configuration
public class KafkaConfig {

    private static final String SASL_TRUSTSTORE_LOCATION = "ssl.truststore.location";
    private static final String SASL_TRUSTSTORE_PWORD = "ssl.truststore.password";

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootStrapServer;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.retryMaxIntervalSecs}")
    private Integer retryMaxIntervalSecs;

    @Value("${spring.kafka.consumer.maxElapsedTime}")
    private Integer maxElapsedTime;

    @Value("${spring.kafka.consumer.retryInitialIntervalSecs}")
    private Integer retryInitialIntervalSecs;

    @Value("${spring.kafka.consumer.pollTimeout}")
    private Integer pollTimeout;

    @Value("${spring.kafka.consumer.username}")
    private String username;
    @Value("${spring.kafka.consumer.password}")
    private String password;
    @Value("${spring.kafka.consumer.login-module}")
    private String loginModule;
    @Value("${spring.kafka.consumer.sasl-mechanism}")
    private String saslMechanism;
    @Value("${spring.kafka.consumer.security-protocol}")
    private String securityProtocol;
    @Value("${spring.kafka.consumer.truststore-location}")
    private String truststoreLocation;
    @Value("${spring.kafka.consumer.truststore-password}")
    private String truststorePassword;
    @Value("${spring.kafka.consumer.offset-auto-reset}")
    private String consumerOffsetAutoReset;
    @Value("${spring.kafka.consumer.concurrency}")
    private int consumerConcurrency;
    @Value("${spring.kafka.consumer.request-timeout}")
    private int consumerRequestTimeout;
    @Value("${spring.kafka.consumer.session-timeout}")
    private int consumerSessionTimeout;
    @Value("${spring.kafka.consumer.metadata-max-age}")
    private int consumerMetadataMaxAge;
    @Value("${spring.kafka.consumer.metadata-max-idle}")
    private int consumerMetadataMaxIdle;
    @Value("${spring.kafka.consumer.max-poll-interval}")
    private int consumerMaxPollInterval;
    @Value("${spring.kafka.consumer.max-poll-records}")
    private int consumerMaxPollRecords;
    @Value("${spring.kafka.consumer.client-config-id}")
    private String kafkaClientId;
    @Value("${spring.kafka.producer.acks-config}")
    private String producerAcksConfig;
    @Value("${spring.kafka.producer.linger}")
    private int producerLinger;
    @Value("${spring.kafka.producer.request-timeout}")
    private int producerRequestTimeout;
    @Value("${spring.kafka.producer.metadata-max-age}")
    private int producerMetadataMaxAge;
    @Value("${spring.kafka.producer.metadata-max-idle}")
    private int producerMetadataMaxIdle;
    @Value("${spring.kafka.producer.batch-size}")
    private int producerBatchSize;
    @Value("${spring.kafka.producer.send-buffer}")
    private int producerSendBuffer;

    @Value("${spring.kafka.consumer.max-fetch-bytes-per-partition}")
    private String consumerMaxFetchBytesPerPartition;
    @Value("${spring.kafka.consumer.max-fetch-bytes-overall}")
    private String consumerMaxFetchBytesOverall;

    @Autowired
    MeterRegistry meterRegistry;

    @Bean
    public Map<String, Object> consumerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerOffsetAutoReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeout);
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, consumerMetadataMaxAge);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerMaxFetchBytesPerPartition);
        addSaslProperties(props, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(props, truststoreLocation, truststorePassword);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Standard Timeouts
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, consumerMetadataMaxIdle); // =180000 (3 min)
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, consumerMetadataMaxAge); // =180000 (3 min)
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeout); // =60000 (1 min)
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerSessionTimeout); // =60000 (1 min)

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerOffsetAutoReset); // =earliest
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual ack in listener,
                    // max 4 ack per partition per sec

        // Polling config, adjust as per processing speed
        // Eg: If listener can process 500 messages in 1 min, then set 500 + 2 min
        // Keep max poll record 500 or less, for controlled error handling and memory optimization
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerMaxPollInterval);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerMaxPollRecords);

        // Background fetch config, adjust as per processing speed
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerMaxFetchBytesPerPartition);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, consumerMaxFetchBytesOverall);

        addSaslProperties(props, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(props, truststoreLocation, truststorePassword);
        var factory = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new StringDeserializer());
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootStrapServer);
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);

        configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerLinger);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeout);
        configProps.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, producerMetadataMaxAge);
        configProps.put(ProducerConfig.METADATA_MAX_IDLE_CONFIG, producerMetadataMaxIdle);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        configProps.put(ProducerConfig.SEND_BUFFER_CONFIG, producerSendBuffer);
        configProps.put(ProducerConfig.ACKS_CONFIG, producerAcksConfig);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId);
        addSaslProperties(configProps, saslMechanism, securityProtocol, loginModule, username, password);
        addTruststoreProperties(configProps, truststoreLocation, truststorePassword);

        return new DefaultKafkaProducerFactory<>(configProps);
    }
    /**
     * This method is used to add SASL properties.
     *
     * @param properties       of type Map.
     * @param saslMechanism    of type String.
     * @param securityProtocol of type String.
     * @param loginModule      of type String.
     * @param username         of type String.
     * @param password         of type String.
     */
    public static void addSaslProperties(Map<String, Object> properties, String saslMechanism, String securityProtocol, String loginModule, String username, String password) {
        if (!StringUtils.isEmpty(username)) {
            properties.put(SECURITY_PROTOCOL_CONFIG, securityProtocol);
            properties.put(SASL_MECHANISM, saslMechanism);
            properties.put(SASL_JAAS_CONFIG, String.format("%s required username=\"%s\" password=\"%s\" ;", loginModule, username, password));
        }
    }

    /**
     * This method is used to store trust store properties.
     *
     * @param properties of type Map.
     * @param location   of type String.
     * @param password   of type String.
     */
    public static void addTruststoreProperties(Map<String, Object> properties, String location, String password) {
        if (!StringUtils.isEmpty(location)) {
            properties.put(SASL_TRUSTSTORE_LOCATION, location);
            properties.put(SASL_TRUSTSTORE_PWORD, password);
        }
    }
}
