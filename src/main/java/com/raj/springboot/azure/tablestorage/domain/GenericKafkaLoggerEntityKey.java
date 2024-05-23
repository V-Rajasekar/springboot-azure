package com.raj.springboot.azure.tablestorage.domain;

import java.io.Serializable;
import java.util.Objects;

public class GenericKafkaLoggerEntityKey implements Serializable {

    private String partitionKey;
    private String partitionNumber;
    private String offset;
    private String week_nr;

    public GenericKafkaLoggerEntityKey() {
        super();
    }

    public GenericKafkaLoggerEntityKey(String partitionKey, String partitionNumber, String offset, String week_nr) {
        super();
        this.partitionKey = partitionKey;
        this.partitionNumber = partitionNumber;
        this.offset = offset;
        this.week_nr = week_nr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        GenericKafkaLoggerEntityKey that = (GenericKafkaLoggerEntityKey) o;
        return partitionKey.equals(that.partitionKey) && partitionNumber.equals(that.partitionNumber) && offset.equals(that.offset) && week_nr.equals(that.week_nr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionKey, partitionNumber, offset, week_nr);
    }
}
