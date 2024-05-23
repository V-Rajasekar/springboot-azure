package com.raj.springboot.azure.tablestorage.service;

import com.raj.springboot.azure.tablestorage.configuration.properties.ApplicationConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.ApplicationCredsConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.EventHubConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.TopicConfiguration;
import com.raj.springboot.azure.tablestorage.constants.ApplicationConstants;
import com.raj.springboot.azure.tablestorage.helper.KafkaLoggerHelper;
import com.raj.springboot.azure.tablestorage.model.Message;
import com.raj.springboot.azure.tablestorage.parsers.MessageParser;
import com.raj.springboot.azure.tablestorage.repository.GenericKafkaLoggerRepository;
import com.raj.springboot.azure.tablestorage.repository.TableStorageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class KafkaLoggerService {

    @Resource(name = "consumerProperties")
    Map<String, Object> consumerProperties;

    TableStorageRepository tableStorageRepository;

    @Autowired
    GenericKafkaLoggerRepository genericKafkaLoggerRepository;

    ApplicationConfiguration configuration;

    ApplicationCredsConfiguration credConfiguration;

    MessageParser messageParser;

    private String loginModule;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Value("${application.deleteTableTask.tableRetention}")
    private long tableRetentionInWeek;

    @Value("${spring.kafka.consumer.pollTimeout}")
    private Integer pollTimeout;

    @Value("${tablestoragelogging.enabled}")
    private Boolean tablestoragelogging;

    @Value("${postgreslogging.enabled}")
    private Boolean postgreslogging;

    Environment environment;

    private static final Logger LOG = LoggerFactory.getLogger(KafkaLoggerService.class);

    @Autowired
    KafkaLoggerService(TableStorageRepository tableStorageRepository,
                       ApplicationConfiguration configuration,
                       ApplicationCredsConfiguration credConfiguration,
                       Environment environment,
                       @Value("${spring.kafka.consumer.login-module}") String loginModule,
                       MessageParser messageParser) {
        this.tableStorageRepository = tableStorageRepository;
        this.configuration = configuration;
        this.credConfiguration = credConfiguration;
        this.environment = environment;
        this.loginModule = loginModule;
        this.messageParser = messageParser;
        this.configuration.getEventHubNamespaces().forEach(e -> {
            e.setPassword(credConfiguration.getEventHubNamespaces().get(e.getIndex()).getPassword());
            e.setTableStorageConnectionString(credConfiguration.getEventHubNamespaces().get(e.getIndex()).getTableStorageConnectionString());
        });
    }

    @Scheduled(fixedRate = Integer.MAX_VALUE)
    public void saveAllKafkaMessages() {
        try {
            LOG.info("---------------------------Started scheduled invocation to consume all Kafka messages");
            configuration.getEventHubNamespaces().forEach(e -> startKafkaListenersWithConfiguration(e));
            LOG.info("-----------Completed save log task------------");
        } catch (Exception ex) {
            LOG.error("Failed to process kafka topic(s)", ex);
        }

    }

    //TODO Disabled to fix delete tables
    @Scheduled(cron = "${application.deleteTableTask.interval}", zone = "Europe/Paris")
    public void deleteTables() {
        try {
            int currentWeekNr = Integer.parseInt(String.valueOf(OffsetDateTime.now(ZoneId.of("CET")).get(WeekFields.ISO.weekOfYear())));
            LOG.info("-----------Started delete table task------------");
            for (EventHubConfiguration e : configuration.getEventHubNamespaces()) {
                List<String> loadedTables = tableStorageRepository.loadTablesName(e);
                List<TopicConfiguration> topics = e.getTopics();
                for (TopicConfiguration topic : topics) {
                    String table = topic.getTable();
                    Period tableRetention = topic.getTableRetention();

                    List<Integer> allTableWeeksNo = KafkaLoggerHelper.createAllTableWeekNo(loadedTables, table);
                    LOG.debug("All table: {}{} ", table, allTableWeeksNo);
                    List<Integer>
                            tableWeeks =
                            KafkaLoggerHelper.createNonRetentionTableWeekNo(allTableWeeksNo,
                                    currentWeekNr,
                                    tableRetention != null ? tableRetention.getDays() / 7 : tableRetentionInWeek);

                    List<String> nonRetentionTableWeeks = KafkaLoggerHelper.addLeadingZeroInSingleDigit(tableWeeks);
                    if (!CollectionUtils.isEmpty(nonRetentionTableWeeks)) {
                        LOG.info("List of nonRetentionTable Weeks {}{}", table, nonRetentionTableWeeks);
                        List<String>
                                nonRetentionTables =
                                nonRetentionTableWeeks.stream().map(weekNo -> table + weekNo).collect(Collectors.toList());
                        nonRetentionTables.forEach(deleteTable -> tableStorageRepository.deleteTable(e, deleteTable));
                    }
                }
            }
            LOG.info("-----------Completed delete table task------------");
        } catch (Exception ex) {
            LOG.error("Failed to delete tables", ex);
        }
    }

    public void startKafkaListenersWithConfiguration(EventHubConfiguration eventHubConfiguration) {
        eventHubConfiguration.getTopics().forEach(topicConfiguration -> {
                    LOG.debug("saveAllKafkaMessagesInNamespace for topic {}", topicConfiguration.getName());
                    ContainerProperties listenerContainerProperties = new ContainerProperties(topicConfiguration.getName());
                    listenerContainerProperties.setPollTimeout(pollTimeout);
                    listenerContainerProperties.setIdleBetweenPolls(500);
                    ConcurrentMessageListenerContainer<String, String> listenerContainer =
                            new ConcurrentMessageListenerContainer<>(consumerFactory, listenerContainerProperties);
                    listenerContainer.setConcurrency(topicConfiguration.getConsumerConcurrency());

                    LOG.debug("saveAllKafkaMessagesInNamespace for topic: {}; with consumer concurrency: {}", topicConfiguration.getName(),
                            listenerContainer.getConcurrency());
                    listenerContainer.setupMessageListener(
                            (BatchAcknowledgingMessageListener<String, String>)
                                    this::saveMessagesToTableStorage);
                    listenerContainer.start();
                }
        );
    }

    public void saveMessagesToTableStorage(List<ConsumerRecord<String, String>> records,
                                           Acknowledgment acknowledgment) {

        if ((records == null) || (records.isEmpty())) return;


        List<Message> messages = new ArrayList<>();
        EventHubConfiguration eventHubConfiguration = configuration.getEventHubNamespaces().get(0);
        List<TopicConfiguration> topicConfigList = eventHubConfiguration.getTopics();
        String topicName = records.get(0).topic();
        Optional<TopicConfiguration> topicResult = topicConfigList.stream()
                .filter(t -> t.getName().equalsIgnoreCase(topicName)).findFirst();

        if (!topicResult.isPresent()) {
            LOG.warn("Missing topic configuration for topicName: {}", topicName);
            return;
        }
        TopicConfiguration topicConf = topicResult.get();

        if(records.get(0).offset() % 200 == 0)
            LOG.info("Topic:{} Number of messages received in the batch :{}", topicConf.getName(), records.size());
        else
            LOG.debug("Topic:{} Number of messages received in the batch :{}", topicConf.getName(), records.size());

        records.forEach(record -> {
                    messages.add(mapMessage(record, topicConf));
                }
        );
        try {
            if (tablestoragelogging) {
                tableStorageRepository.saveRecords(eventHubConfiguration, topicConf, messages);
            }

            if (postgreslogging) {
                messages.forEach(msg -> {
                            try {
                                MDC.put(ApplicationConstants.OFFSET, String.valueOf(msg.getOffset()));
                                MDC.put(ApplicationConstants.PARTITION, String.valueOf(msg.getPartition()));
                                MDC.put(ApplicationConstants.TOPIC, String.valueOf(msg.getTopic()));
                                String partitionKey = msg.getFields().get("PartitionKey") == null ? null : msg.getFields().get("PartitionKey");
                                MDC.put(ApplicationConstants.PARTITION_KEY, partitionKey);

                                String consItemNo = msg.getFields().get("consItemNo") == null ? null : msg.getFields().get("consItemNo");
                                String consNo = msg.getFields().get("consNo") == null ? null : msg.getFields().get("consNo");
                                String operation = msg.getFields().get("operation") == null ? null : msg.getFields().get("operation");
                                String auditCreatedTsStr = msg.getFields().get("auditCreatedTs") == null ? null : msg.getFields().get("auditCreatedTs");
                                String auditModifiedTsStr = msg.getFields().get("auditModifiedTs") == null ? null : msg.getFields().get("auditModifiedTs");
                                String auditCreatedSvc = msg.getFields().get("auditCreatedSvc") == null ? null : msg.getFields().get("auditCreatedSvc");
                                String auditModifiedSvc = msg.getFields().get("auditModifiedSvc") == null ? null : msg.getFields().get("auditModifiedSvc");

                                String consItemUniqueId = msg.getFields().get("consignmentItemUniqueId") == null ? null : msg.getFields().get("consignmentItemUniqueId");
                                if(isBlank(consItemUniqueId)
                                        && null != msg.getFields().get("consItemUniqId"))
                                    consItemUniqueId = msg.getFields().get("consItemUniqId");

                                String consUniqueId = msg.getFields().get("consignmentUniqueId") == null ? null : msg.getFields().get("consignmentUniqueId");
                                if(isBlank(consUniqueId)
                                        && null != msg.getFields().get("consUniqueId"))
                                    consUniqueId = msg.getFields().get("consUniqueId");
                                if(isBlank(consUniqueId)
                                        && null != msg.getFields().get("consUniqId"))
                                    consUniqueId = msg.getFields().get("consUniqId");

                                OffsetDateTime auditCreatedTs = null;
                                OffsetDateTime auditModifiedTs = null;
                                if (isNotBlank(auditCreatedTsStr)) {
                                    try {
                                        auditCreatedTs = OffsetDateTime.parse(auditCreatedTsStr);
                                    } catch (Exception dtParseExp) {
                                        LOG.error("Error parsing auditCreatedTsStr from {}", auditCreatedTsStr, dtParseExp);
                                    }
                                }
                                if (isNotBlank(auditModifiedTsStr)) {
                                    try {
                                        auditModifiedTs = OffsetDateTime.parse(auditModifiedTsStr);
                                    } catch (Exception dtParseExp) {
                                        LOG.error("Error parsing auditCreatedTsStr from {}", auditModifiedTsStr, dtParseExp);
                                    }
                                }
                                if (isNotBlank(auditCreatedSvc)
                                        && auditCreatedSvc.length() > 5) {
                                    auditCreatedSvc = auditCreatedSvc.trim().substring(0, 4);
                                }
                                if (isNotBlank(auditModifiedSvc)
                                        && auditModifiedSvc.length() > 5) {
                                    auditModifiedSvc = auditModifiedSvc.trim().substring(0, 4);
                                }

                                switch (topicConf.getTable()) {
                                    case "oem_eve_ci_publish":
                                        if (isBlank(consNo)) {
                                            consNo = partitionKey;
                                        }
                                        genericKafkaLoggerRepository.insertConsItemEve(
                                                consItemNo,
                                                consNo,
                                                OffsetDateTime.now(),
                                                msg.getPayload(),
                                                consItemUniqueId,
                                                consUniqueId,
                                                operation,
                                                auditCreatedTs,
                                                auditCreatedSvc,
                                                auditModifiedTs,
                                                auditModifiedSvc,
                                                getCurrentWeekNr(),
                                                msg.getOffset(),
                                                msg.getPartition());
                                        break;
                                    case "oem_eve_c_publish":
                                        if (isBlank(consNo)) {
                                            consNo = partitionKey;
                                        }
                                        genericKafkaLoggerRepository.insertConsEve(
                                                consNo,
                                                OffsetDateTime.now(),
                                                msg.getPayload(),
                                                consUniqueId,
                                                operation,
                                                auditCreatedTs,
                                                auditCreatedSvc,
                                                auditModifiedTs,
                                                auditModifiedSvc,
                                                getCurrentWeekNr(),
                                                msg.getOffset(),
                                                msg.getPartition());
                                        break;
                                    case "oem_ord_ci_publish":
                                        if (isBlank(consNo)) {
                                            consNo = partitionKey;
                                        }
                                        genericKafkaLoggerRepository.insertConsItemOrd(
                                                consItemNo,
                                                consNo,
                                                OffsetDateTime.now(),
                                                msg.getPayload(),
                                                consItemUniqueId,
                                                consUniqueId,
                                                operation,
                                                auditCreatedTs,
                                                auditCreatedSvc,
                                                auditModifiedTs,
                                                auditModifiedSvc,
                                                getCurrentWeekNr(),
                                                msg.getOffset(),
                                                msg.getPartition());
                                        break;
                                    case "oem_ord_c_publish":
                                        if (isBlank(consNo)) {
                                            consNo = partitionKey;
                                        }
                                        genericKafkaLoggerRepository.insertConsOrd(
                                                consNo,
                                                OffsetDateTime.now(),
                                                msg.getPayload(),
                                                consUniqueId,
                                                operation,
                                                auditCreatedTs,
                                                auditCreatedSvc,
                                                auditModifiedTs,
                                                auditModifiedSvc,
                                                getCurrentWeekNr(),
                                                msg.getOffset(),
                                                msg.getPartition());
                                        break;

                                }
                            } catch (Exception ex) {
                                LOG.error("Error while saving message: {}", ex.getMessage(), ex);
                            } finally {
                                MDC.remove(ApplicationConstants.OFFSET);
                                MDC.remove(ApplicationConstants.PARTITION);
                                MDC.remove(ApplicationConstants.TOPIC);
                                MDC.remove(ApplicationConstants.PARTITION_KEY);
                            }
                        }
                );
            }

        } catch (Exception e) {
            LOG.error("Error processing record", e);
        }

        messages.clear();
        if (acknowledgment != null && records.get(0).offset() % 19 == 0) {
            LOG.debug("Acknowledged messages");
            acknowledgment.acknowledge();
        }
    }

    private String getCurrentWeekNr() {
        return DateTimeFormatter.ofPattern("ww").format(OffsetDateTime.now().toLocalDate());
    }

    public Message mapMessage(ConsumerRecord<String, String> record, TopicConfiguration topic) {
        Message msg = new Message();
        try {
            MDC.put(ApplicationConstants.OFFSET, String.valueOf(record.offset()));
            MDC.put(ApplicationConstants.PARTITION, String.valueOf(record.partition()));
            MDC.put(ApplicationConstants.TOPIC, String.valueOf(record.topic()));
            MDC.put(ApplicationConstants.PARTITION_KEY, String.valueOf(record.key()));
            LOG.debug("Message Processed,  topic = {} , partition = {} offset = {}, key = {}, timestamp = {}", record.topic(), record.partition(), record.offset(), record.key(), record.timestamp());

            msg.setTopic(record.topic());
            msg.setPartition(record.partition());
            msg.setOffset(record.offset());
            msg.setPayload(record.value());
            if (record.timestamp() != 0) {
                msg.setMsgTimestamp(OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.of("UTC")));
            }

            messageParser.loadFieldsToMessage(msg, topic);
            messageParser.getHeaders(msg, topic, record);

            if (msg.getFields().get("PartitionKey") == null) {
                String partitionKey = record.key();
                if (partitionKey == null) {
                    partitionKey = record.partition() + "_" + record.offset();
                }
                msg.getFields().put("PartitionKey", partitionKey);

                if (record.offset() % 90 == 0)
                    LOG.warn("PartitionKey not found and overridden for Topic:{}; with value {}", topic.getName(), partitionKey);
                else
                    LOG.debug("PartitionKey not found and overridden for Topic:{}; with value {}", topic.getName(), partitionKey);
            }
        } catch (Exception ex) {
            LOG.error("Error while parsing message: {}", ex.getMessage(), ex);
        } finally {
            MDC.remove(ApplicationConstants.OFFSET);
            MDC.remove(ApplicationConstants.PARTITION);
            MDC.remove(ApplicationConstants.TOPIC);
            MDC.remove(ApplicationConstants.PARTITION_KEY);
        }
        return msg;
    }

}
