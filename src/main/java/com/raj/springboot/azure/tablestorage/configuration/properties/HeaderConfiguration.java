package com.raj.springboot.azure.tablestorage.configuration.properties;

public class HeaderConfiguration {

    String path;
    String column;

    public String getColumn() {
        return column;
    }
    public void setColumn(String column) {
        this.column = column;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}
