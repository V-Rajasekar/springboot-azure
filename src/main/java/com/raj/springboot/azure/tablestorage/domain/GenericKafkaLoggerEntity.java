package com.raj.springboot.azure.tablestorage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kafka_logs")
public class GenericKafkaLoggerEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "partition_number")
    private String partitionNumber;

    @Column(name = "partition_key")
    String partitionKey;

    @Column(name = "offset")
    String offset;

    @Column(name = "eventhub_name")
    String eventHubName;

    @Column(name = "consignment_item_unique_id")
    String itemUniqueId;

    @Column(name = "consignment_unique_id")
    String consignmentUniqueId;

    @Column(name = "payload")
    String payLoad;

    @Column(name = "operation")
    String Operation;

    @Column(name = "created_timestamp")
    OffsetDateTime createdTs;

    @Column(name = "week_nr")
    String weekNr;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getEventHubName() {
        return eventHubName;
    }

    public void setEventHubName(String eventHubName) {
        this.eventHubName = eventHubName;
    }

    public String getItemUniqueId() {
        return itemUniqueId;
    }

    public void setItemUniqueId(String itemUniqueId) {
        this.itemUniqueId = itemUniqueId;
    }

    public String getConsignmentUniqueId() {
        return consignmentUniqueId;
    }

    public void setConsignmentUniqueId(String consignmentUniqueId) {
        this.consignmentUniqueId = consignmentUniqueId;
    }

    public String getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(String payLoad) {
        this.payLoad = payLoad;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
    }

    public String getOperation() {
        return Operation;
    }

    public void setOperation(String operation) {
        Operation = operation;
    }

    public String getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(String partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    public String getWeekNr() {
        return weekNr;
    }

    public void setWeekNr(String weekNr) {
        this.weekNr = weekNr;
    }
}
