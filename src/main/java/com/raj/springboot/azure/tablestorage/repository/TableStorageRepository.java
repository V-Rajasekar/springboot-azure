package com.raj.springboot.azure.tablestorage.repository;

import com.azure.core.http.rest.Response;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableTransactionAction;
import com.azure.data.tables.models.TableTransactionActionType;
import com.azure.data.tables.models.TableTransactionFailedException;
import com.azure.data.tables.models.TableTransactionResult;
import com.raj.springboot.azure.tablestorage.configuration.TableClientProvider;
import com.raj.springboot.azure.tablestorage.configuration.properties.EventHubConfiguration;
import com.raj.springboot.azure.tablestorage.configuration.properties.TopicConfiguration;
import com.raj.springboot.azure.tablestorage.model.Message;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TableStorageRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TableStorageRepository.class);

    //AZ table storage has the table storage limitation of 1 MB/Table entiry and 62KB per column as the dats's are
    // saved in UTF-16 format the limits are reduced to half here.
    private static final int MAX_TABLE_COLUMN_LENGTH = 32768;

    private static final int MAX_MESSAGE_LENGTH = 512000;
    
    @Value("${application.deleteTableTask.timeout}")
    private long timeout;

    @Autowired
    private TableClientProvider tableClientProvider;

    private Map<String, TableClient> tableClientMap = new HashMap<>();

    public void saveRecords(EventHubConfiguration eventHubNamespace, TopicConfiguration topic, List<Message> messages) throws IOException {
        String partitionKey = null;
        String tableName = null;
        try {
            TableClient tableClient = null;
            int week = messages.get(0).getMsgTimestamp().atZoneSameInstant(ZoneId.of("CET")).get(WeekFields.ISO.weekOfYear());
            if (week < 10)
                tableName = topic.getTable() + "0" + week;
            else
                tableName = topic.getTable() + week;
            if (tableClientMap.get(tableName)!=null){
                tableClient = tableClientMap.get(tableName);
            } else {
                TableServiceClient  tableServiceClient = tableClientProvider.getTableServiceClientReference(eventHubNamespace.getTableStorageConnectionString());
                tableServiceClient.createTableIfNotExists(tableName);
                tableClient = tableClientProvider.getTableClientReference(eventHubNamespace.getTableStorageConnectionString(),tableName);
                tableClientMap.put(tableName,tableClient);
            }

            List<TableTransactionAction> transactionActions = createTableTransactionActions(messages);

            while (transactionActions.size() > 0 ) {
                List<TableTransactionAction> transactionActionsForTransaction = new ArrayList<>();
                partitionKey = transactionActions.get(0).getEntity().getPartitionKey();
                for (TableTransactionAction transactionAction: transactionActions) {
                    if ((transactionAction.getEntity().getPartitionKey().equals(partitionKey))&&(transactionActionsForTransaction.size()<100)){
                        transactionActionsForTransaction.add(transactionAction);
                    }
                }

                //Submit the batch of operations and inspect the status codes for every action.
                if (!CollectionUtils.isEmpty(transactionActionsForTransaction)) {
                    TableTransactionResult tableTransactionResult = tableClient.submitTransaction(transactionActionsForTransaction);
                    tableTransactionResult.getTransactionActionResponses().forEach(tableTransactionActionResponse -> {
                        if (tableTransactionActionResponse.getStatusCode() != 204)
                            LOG.info("Failed saving Request {}, with http response code {}", tableTransactionActionResponse.getRequest(), String.valueOf(tableTransactionActionResponse.getStatusCode()));
                    });
                    transactionActions.removeAll(transactionActionsForTransaction);
                }
            }
        } catch (TableTransactionFailedException e) {
            LOG.error("Error while saving records to Table storage with the error message/{}:{}",
                      e.getValue().getErrorMessage(), e);
        }catch (Exception ex){
            LOG.error("Failing for the table name/{} and partitionKey/{}", tableName, partitionKey);
            LOG.error("Error while saving records to Table storage.", ex);
        }
    }

    private List<TableTransactionAction> createTableTransactionActions(List<Message> messages) throws IOException {
        if (!CollectionUtils.isEmpty(messages)) {
            List<TableTransactionAction> transactionActions = new ArrayList<>();
            for (Message message : messages) {
                String payload = message.getPayload();
                if (StringUtils.isNotBlank(payload)) {

                    //Truncating to 500K because of storage limitation, may be the exceeding payloads can be written
                    // in a separate row
                    if (payload.length() > MAX_MESSAGE_LENGTH) {
                        payload = payload.substring(0, MAX_MESSAGE_LENGTH-1);
                    }
                    List<String> payloads = createPayloadsByByteLength(payload,
                                                                       StandardCharsets.UTF_8.toString(),
                                                                       MAX_TABLE_COLUMN_LENGTH);
                    int numberOfPayloads = payloads.size();
                    String
                            rowKey =
                            message.getMsgTimestamp().atZoneSameInstant(ZoneId.of("CET")).getDayOfWeek().getValue() + StringUtils.leftPad(String.valueOf(message.getMsgTimestamp()
                                                                                                                                                                .atZoneSameInstant(ZoneId.of(
                                                                                                                                                                        "CET"))
                                                                                                                                                                .getHour()), 2, "0");
                    String partitionKey = message.getPartition() + "_" + message.getOffset();
                    TableEntity
                            tableEntity =
                            new TableEntity(rowKey, partitionKey).addProperty("Partition", message.getPartition())
                                                                 .addProperty("Offset", message.getOffset())
                                                                 .addProperty("MsgTimestamp", message.getMsgTimestamp())
                                                                 .addProperty("NumOfPayloadFields", numberOfPayloads);

                    tableEntity.addProperty("Payload", payloads.get(0));
                    for (int i = 1; i < numberOfPayloads; i++) {
                        tableEntity.addProperty("Payload" + i, payloads.get(i));
                    }

                    message.getFields().forEach(tableEntity::addProperty);
                    message.getHeaders().forEach(tableEntity::addProperty);
                    transactionActions.add(new TableTransactionAction(TableTransactionActionType.UPSERT_REPLACE,
                                                                      tableEntity));
                }
            }
            return transactionActions;
        }
        return Collections.emptyList();
    }

    public static List<String> createPayloadsByByteLength(String message,String encoding, int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);
        CharBuffer in = CharBuffer.wrap(message);
        List<String> payloads = new ArrayList<>();
        int pos = 0;
        while(true) {
            CoderResult cr = coder.encode(in, out, true);
            int newpos = message.length() - in.length();
            String s = message.substring(pos, newpos);
            payloads.add(s);
            pos = newpos;
            out.rewind();
            if (! cr.isOverflow()) {
                break;
            }
        }
        return payloads;
    }

    public List<String> loadTablesName(EventHubConfiguration eventHubNamespace) {
        TableServiceClient
                tableServiceClientReference =
                tableClientProvider.getTableServiceClientReference(eventHubNamespace.getTableStorageConnectionString());
        return
                tableServiceClientReference.listTables()
                                           .stream()
                                           .map(tableItem -> tableItem.getName())
                                           .collect(Collectors.toList());
    }

    public void deleteTable(EventHubConfiguration eventHubNamespace, String tableName) {
        try {
            TableServiceClient
                    tableServiceClient =
                    tableClientProvider.getTableServiceClientReference(eventHubNamespace.getTableStorageConnectionString());
            Response<Void>
                    voidResponse =
                    tableServiceClient.deleteTableWithResponse(tableName, Duration.ofMillis(timeout), null);
            if (HttpStatus.NO_CONTENT.value() == voidResponse.getStatusCode() && tableClientMap.containsKey(tableName)){
                tableClientMap.remove(tableName);
            }
            LOG.info("Delete table/{} with response code {}", tableName, voidResponse.getStatusCode());

        } catch (Exception ex) {
            LOG.error("Unable to delete table/{}. Root cause for failure {} ", tableName, ex.getCause());
        }
    }
}
