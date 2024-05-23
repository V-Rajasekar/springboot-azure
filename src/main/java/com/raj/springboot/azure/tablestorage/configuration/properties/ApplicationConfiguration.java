package com.raj.springboot.azure.tablestorage.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "application")
@Component
public class ApplicationConfiguration {

    List<EventHubConfiguration> eventHubNamespaces;

    public List<EventHubConfiguration> getEventHubNamespaces() {
        return eventHubNamespaces;
    }

    public void setEventHubNamespaces(List<EventHubConfiguration> eventHubNamespaces) {
        this.eventHubNamespaces = eventHubNamespaces;
    }

}
