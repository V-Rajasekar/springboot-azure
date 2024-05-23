package com.raj.springboot.azure.tablestorage.configuration.properties;

import java.util.List;

public class EventHubCredsConfiguration {
    String password;
    String tableStorageConnectionString;


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
}
