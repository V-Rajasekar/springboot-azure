package com.raj.springboot.azure.tablestorage.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

public class Message {
    Long offset;
    Integer partition;
    String topic;
    String payload;
    OffsetDateTime msgTimestamp;
    Map<String, String> fields;
    Map<String, String> headers;


    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return offset.equals(message.offset) && partition.equals(message.partition) && topic.equals(message.topic) && payload.equals(message.payload) && msgTimestamp.equals(message.msgTimestamp) && Objects.equals(fields, message.fields) && Objects.equals(headers, message.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, partition, topic, payload, msgTimestamp, fields, headers);
    }

    @Override
    public String toString() {
        return "Message{" +
                "offset=" + offset +
                ", partition=" + partition +
                ", topic='" + topic + '\'' +
                ", payload='" + payload + '\'' +
                ", msgTimestamp=" + msgTimestamp +
                ", fields=" + fields +
                ", headers=" + headers +
                '}';
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public OffsetDateTime getMsgTimestamp() {
        return msgTimestamp;
    }

    public void setMsgTimestamp(OffsetDateTime msgTimestamp) {
        this.msgTimestamp = msgTimestamp;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() { return headers; }

    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

}
