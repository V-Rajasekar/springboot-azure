package com.raj.springboot.azure.tablestorage.parsers;

import com.jayway.jsonpath.DocumentContext;
import com.raj.springboot.azure.tablestorage.configuration.properties.FieldConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.HeaderConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.TopicConfiguration;
import com.raj.springboot.azure.tablestorage.model.Message;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

@Component
public class MessageParser {

    XpathParser xpathParser;

    JsonPathParser jsonPathParser;

    FixedWidthParser fixedWidthParser;

    DelimitedFieldParser delimitedFieldParser;

    private static final Logger LOG = LoggerFactory.getLogger(MessageParser.class);

    @Autowired
    public MessageParser(XpathParser xpathParser, JsonPathParser jsonPathParser, FixedWidthParser fixedWidthParser, DelimitedFieldParser delimitedFieldParser) {
        this.xpathParser = xpathParser;
        this.jsonPathParser = jsonPathParser;
        this.fixedWidthParser = fixedWidthParser;
        this.delimitedFieldParser = delimitedFieldParser;
    }

    public void loadFieldsToMessage(Message msg, TopicConfiguration topicConfiguration){
        Map<String, String> fieldValues = new HashMap<>();
        getFieldsFromJson(msg,topicConfiguration, fieldValues);
        getFieldsFromXML(msg,topicConfiguration, fieldValues);
        getFieldsFromFixedWidth(msg,topicConfiguration, fieldValues);
        getWaybillFieldsFromFixedWidth(msg,topicConfiguration, fieldValues);
        getFieldsFromDelimited(msg,topicConfiguration, fieldValues);
        msg.setFields(fieldValues);
    }

    public void getHeaders(Message message, TopicConfiguration topicConfiguration, ConsumerRecord<String, String> record) {
        Map<String, String> headerValues = new HashMap<>();
        getHeadersValueFromKafkaHeader(message, topicConfiguration, record, headerValues);
        message.setHeaders(headerValues);
    }

    private  void getFieldsFromJson(Message msg, TopicConfiguration topicConfiguration,  Map<String, String> fieldValues){
        if (topicConfiguration.getMsgType().equals("json")){
            try {
                DocumentContext jsonDocument = jsonPathParser.getJsonDocument(msg.getPayload());
                if ((topicConfiguration.getFields() != null) && (jsonDocument != null)) {
                    for (FieldConfiguration field : topicConfiguration.getFields()) {
                        fieldValues.put(field.getColumn(), jsonPathParser.getField(jsonDocument, field.getPath(), msg.getTopic()));
                    }
                }
            }  catch (Exception ex) {
                LOG.warn("Error while parsing the message for topic {} , partition {} , offset {}. Exception {}", msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
            }
        }
    }
    private  void getFieldsFromXML(Message msg, TopicConfiguration topicConfiguration,  Map<String, String> fieldValues){
        if (topicConfiguration.getMsgType().equals("xml")){
            try {
                Document xmlDocument = xpathParser.getXmlDocument(msg.getPayload());
                if ((topicConfiguration.getFields()!=null)&&(xmlDocument !=null)) {
                    for (FieldConfiguration field : topicConfiguration.getFields()) {
                        fieldValues.put(field.getColumn(), xpathParser.getField(xmlDocument, field.getPath(), msg.getTopic()));
                    }
                }
            } catch (Exception ex) {
                LOG.warn("Error while parsing the message for topic {} , partition {} , offset {}. Exception {}", msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
            }

        }
    }
    private  void getFieldsFromFixedWidth(Message msg, TopicConfiguration topicConfiguration,  Map<String, String> fieldValues){
        if (topicConfiguration.getMsgType().equals("fixed") && !topicConfiguration.getName().equalsIgnoreCase("ph-oem-ord-waybill")){
            try {
                if ((topicConfiguration.getFields() != null) && (msg.getPayload() != null)) {
                    for (FieldConfiguration field : topicConfiguration.getFields()) {
                       LOG.info("Field Column {} startIndex/{} length:/{}", field.getColumn(), field.getStartIndex(), field.getLength());
                        fieldValues.put(field.getColumn(), fixedWidthParser.getField(msg.getPayload(),field.getStartIndex(),field.getLength(), msg.getTopic()));
                    }
                }
            }  catch (Exception ex) {
                LOG.warn("Error while parsing the message for topic {} , partition {} , offset {}. Exception {}", msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
            }
        }
    }

    private  void getWaybillFieldsFromFixedWidth(Message msg, TopicConfiguration topicConfiguration,  Map<String, String> fieldValues){
        if (topicConfiguration.getMsgType().equals("fixed")){

            try {
                if ((topicConfiguration.getFields() != null) && (msg.getPayload() != null)) {
                    for (FieldConfiguration field : topicConfiguration.getFields()) {
                        LOG.debug("Field Column {} startIndex/{} length:/{}", field.getColumn(), field.getStartIndex(), field.getLength());
                        fieldValues.put(field.getColumn(), fixedWidthParser.getField(msg.getPayload(),field.getStartIndex(),field.getLength(), msg.getTopic()));
                        String value = fixedWidthParser.getField(msg.getPayload(), field.getStartIndex(), field.getLength(), msg.getTopic());
                        fieldValues.put(field.getColumn(), value);
                        customParitionKeyConfigForWaybill(topicConfiguration, fieldValues);
                    }
                }
            }  catch (Exception ex) {
                LOG.warn("Error while parsing the message for topic {} , partition {} , offset {}. Exception {}", msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
            }
        }
    }

    /**
     * Method to store the waybill partition key based on the recordId.
     * @param topicConfiguration holds topic configuration
     * @param fieldValues holds the fiele values
     */
    private void customParitionKeyConfigForWaybill(TopicConfiguration topicConfiguration, Map<String, String> fieldValues) {
        if (topicConfiguration.getName().equalsIgnoreCase("ph-oem-ord-waybill")) {
            if (fieldValues.get("recordId").equalsIgnoreCase("N9")) {
                topicConfiguration.getFields().stream()
                        .filter(fieldItem -> fieldItem.getColumn().equalsIgnoreCase("PartitionKey"))
                        .findFirst().ifPresent( partitionKeyField -> partitionKeyField.setStartIndex(67));
            } else {
                topicConfiguration.getFields().stream()
                        .filter(fieldItem -> fieldItem.getColumn().equalsIgnoreCase("PartitionKey"))
                        .findFirst().ifPresent( partitionKeyField -> partitionKeyField.setStartIndex(49));
            }
        }
    }

    private  void getFieldsFromDelimited(Message msg, TopicConfiguration topicConfiguration,  Map<String, String> fieldValues){
        if (topicConfiguration.getMsgType().equals("delimited")){
            try {
                if ((topicConfiguration.getFields() != null) && (msg.getPayload() != null)) {
                    for (FieldConfiguration field : topicConfiguration.getFields()) {
                        fieldValues.put(field.getColumn(), delimitedFieldParser.getField(msg.getPayload(),field.getIndex(),field.getDelimiter(), msg.getTopic()));
                    }
                }
            }  catch (Exception ex) {
                LOG.warn("Error while parsing the message for topic {} , partition {} , offset {}. Exception {}", msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
            }
        }
    }

    public void getHeadersValueFromKafkaHeader(Message msg, TopicConfiguration topicConfiguration, ConsumerRecord<String, String> record, Map<String, String> headerValues) {
        try {
            if (topicConfiguration.getHeaders() != null) {
                for (HeaderConfiguration header : topicConfiguration.getHeaders()) {
                    if (record.headers().headers(header.getPath()).iterator().hasNext()) {
                        headerValues.put(header.getColumn(), new String(record.headers().headers(header.getPath()).iterator().next().value()));
                    }
                }
            }
        } catch (Exception ex) {
          LOG.warn("Error while fetching the header for topic {} , partition {} , offset {}. Exception {}" , msg.getTopic(), msg.getPartition(), msg.getOffset(), ex);
        }
    }
}
