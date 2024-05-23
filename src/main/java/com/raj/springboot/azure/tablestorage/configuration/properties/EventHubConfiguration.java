package com.raj.springboot.azure.tablestorage.configuration.properties;

import java.util.List;

public class EventHubConfiguration {

    int index;
    String bootstrapServers;
    String username;
    String password;
    String tableStorageConnectionString;
    Integer consumerConcurrency;
    List<TopicConfiguration> topics;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTableStorageConnectionString() {
        return tableStorageConnectionString;
    }

    public void setTableStorageConnectionString(String tableStorageConnectionString) {
        this.tableStorageConnectionString = tableStorageConnectionString;
    }

    public List<TopicConfiguration> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicConfiguration> topics) {
        this.topics = topics;
    }

    public Integer getConsumerConcurrency() {
        return consumerConcurrency;
    }

    public void setConsumerConcurrency(Integer consumerConcurrency) {
        this.consumerConcurrency = consumerConcurrency;
    }
}
