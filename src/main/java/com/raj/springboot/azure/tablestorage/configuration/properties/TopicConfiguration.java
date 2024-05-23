package com.raj.springboot.azure.tablestorage.configuration.properties;

import java.time.Period;
import java.util.List;

public class TopicConfiguration {
    String name;
    String table;
    Integer consumerConcurrency;
    String msgType;
    Period tableRetention;
    List<FieldConfiguration> fields;
    List<HeaderConfiguration> headers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Integer getConsumerConcurrency() {
        return consumerConcurrency == null || consumerConcurrency < 1 ? 1 : consumerConcurrency;
    }

    public Period getTableRetention() {
        return tableRetention;
    }

    public List<FieldConfiguration> getFields() {
        return fields;
    }
    public void setFields(List<FieldConfiguration> fields) {
        this.fields = fields;
    }

    public List<HeaderConfiguration> getHeaders() {
        return headers;
    }
    public void setHeaders(List<HeaderConfiguration> headers) {
        this.headers = headers;
    }

    public void setConsumerConcurrency(Integer consumerConcurrency) {
        this.consumerConcurrency = consumerConcurrency;
    }

    public void setTableRetention(Period tableRetention) {
        this.tableRetention = tableRetention;
    }
}

