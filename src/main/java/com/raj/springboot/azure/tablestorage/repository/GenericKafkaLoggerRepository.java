package com.raj.springboot.azure.tablestorage.repository;

import com.raj.springboot.azure.tablestorage.domain.GenericKafkaLoggerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;

@Repository
public interface GenericKafkaLoggerRepository extends JpaRepository<GenericKafkaLoggerEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "insert into oem_eve_ci_publish(cons_item_no, cons_no, created_ts, payload, cons_item_unique_id, cons_unique_id, operation, audit_created_ts, audit_created_svc, audit_modified_ts, audit_modified_svc, wk_nr, kafka_offset, kafka_partition) " +
            "values (:cons_item_no,:cons_no,:created_ts,cast(:payload as jsonb),:cons_item_unique_id,:cons_unique_id,:operation,:audit_created_ts,:audit_created_svc,:audit_modified_ts,:audit_modified_svc,:wk_nr,:kafka_offset,:kafka_partition) "
            , nativeQuery = true)
    void insertConsItemEve(@Param("cons_item_no") String consItemNo,
                           @Param("cons_no") String consNo,
                           @Param("created_ts") OffsetDateTime createdTs,
                           @Param("payload") String payload,
                           @Param("cons_item_unique_id") String consItemUniqueId,
                           @Param("cons_unique_id") String consUniqueId,
                           @Param("operation") String operation,
                           @Param("audit_created_ts") OffsetDateTime auditCreatedTs,
                           @Param("audit_created_svc") String auditCreatedSvc,
                           @Param("audit_modified_ts") OffsetDateTime auditModifiedTs,
                           @Param("audit_modified_svc") String auditModifiedSvc,
                           @Param("wk_nr") String weekNr,
                           @Param("kafka_offset") Long kafkaOffset,
                           @Param("kafka_partition") Integer kafkaPartition
    );

    @Transactional
    @Modifying
    @Query(value = "insert into oem_eve_c_publish(cons_no, created_ts, payload, cons_unique_id, operation, audit_created_ts, audit_created_svc, audit_modified_ts, audit_modified_svc, wk_nr, kafka_offset, kafka_partition) " +
            "values (:cons_no,:created_ts,cast(:payload as jsonb),:cons_unique_id,:operation,:audit_created_ts,:audit_created_svc,:audit_modified_ts,:audit_modified_svc,:wk_nr,:kafka_offset,:kafka_partition) "
            , nativeQuery = true)
    void insertConsEve(@Param("cons_no") String consNo,
                       @Param("created_ts") OffsetDateTime createdTs,
                       @Param("payload") String payload,
                       @Param("cons_unique_id") String consUniqueId,
                       @Param("operation") String operation,
                       @Param("audit_created_ts") OffsetDateTime auditCreatedTs,
                       @Param("audit_created_svc") String auditCreatedSvc,
                       @Param("audit_modified_ts") OffsetDateTime auditModifiedTs,
                       @Param("audit_modified_svc") String auditModifiedSvc,
                       @Param("wk_nr") String weekNr,
                       @Param("kafka_offset") Long kafkaOffset,
                       @Param("kafka_partition") Integer kafkaPartition
    );

    @Transactional
    @Modifying
    @Query(value = "insert into oem_ord_ci_publish(cons_item_no, cons_no, created_ts, payload, cons_item_unique_id, cons_unique_id, operation, audit_created_ts, audit_created_svc, audit_modified_ts, audit_modified_svc, wk_nr, kafka_offset, kafka_partition) " +
            "values (:cons_item_no,:cons_no,:created_ts,cast(:payload as jsonb),:cons_item_unique_id,:cons_unique_id,:operation,:audit_created_ts,:audit_created_svc,:audit_modified_ts,:audit_modified_svc,:wk_nr,:kafka_offset,:kafka_partition) "
            , nativeQuery = true)
    void insertConsItemOrd(@Param("cons_item_no") String consItemNo,
                           @Param("cons_no") String consNo,
                           @Param("created_ts") OffsetDateTime createdTs,
                           @Param("payload") String payload,
                           @Param("cons_item_unique_id") String consItemUniqueId,
                           @Param("cons_unique_id") String consUniqueId,
                           @Param("operation") String operation,
                           @Param("audit_created_ts") OffsetDateTime auditCreatedTs,
                           @Param("audit_created_svc") String auditCreatedSvc,
                           @Param("audit_modified_ts") OffsetDateTime auditModifiedTs,
                           @Param("audit_modified_svc") String auditModifiedSvc,
                           @Param("wk_nr") String weekNr,
                           @Param("kafka_offset") Long kafkaOffset,
                           @Param("kafka_partition") Integer kafkaPartition
    );

    @Transactional
    @Modifying
    @Query(value = "insert into oem_ord_c_publish(cons_no, created_ts, payload, cons_unique_id, operation, audit_created_ts, audit_created_svc, audit_modified_ts, audit_modified_svc, wk_nr, kafka_offset, kafka_partition) " +
            "values (:cons_no,:created_ts,cast(:payload as jsonb),:cons_unique_id,:operation,:audit_created_ts,:audit_created_svc,:audit_modified_ts,:audit_modified_svc,:wk_nr,:kafka_offset,:kafka_partition) "
            , nativeQuery = true)
    void insertConsOrd(@Param("cons_no") String consNo,
                       @Param("created_ts") OffsetDateTime createdTs,
                       @Param("payload") String payload,
                       @Param("cons_unique_id") String consUniqueId,
                       @Param("operation") String operation,
                       @Param("audit_created_ts") OffsetDateTime auditCreatedTs,
                       @Param("audit_created_svc") String auditCreatedSvc,
                       @Param("audit_modified_ts") OffsetDateTime auditModifiedTs,
                       @Param("audit_modified_svc") String auditModifiedSvc,
                       @Param("wk_nr") String weekNr,
                       @Param("kafka_offset") Long kafkaOffset,
                       @Param("kafka_partition") Integer kafkaPartition
    );

}
