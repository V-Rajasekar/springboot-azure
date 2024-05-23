package com.raj.springboot.azure.tablestorage.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "applicationcreds")
@Component
public class ApplicationCredsConfiguration {
    List<EventHubCredsConfiguration> eventHubNamespaces;

    public List<EventHubCredsConfiguration> getEventHubNamespaces() {
        return eventHubNamespaces;
    }

    public void setEventHubNamespaces(List<EventHubCredsConfiguration> eventHubNamespaces) {
        this.eventHubNamespaces = eventHubNamespaces;
    }
}
