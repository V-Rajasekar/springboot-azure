package com.raj.springboot.azure.tablestorage.configuration;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TableClientProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TableClientProvider.class);

    @Value("${spring.table-storage.connection-string}")
    private String tableStroageConnectionString;

    public TableServiceClient getTableServiceClientReference(String tableStorageConnectionString) {

        try {
            return new TableServiceClientBuilder()
                    .connectionString(tableStorageConnectionString)
                    .buildClient();
        } catch (Exception e) {
            LOG.error("Error creating Table Service Client.",e);
            throw e;
        }
    }

    public TableClient getTableClientReference(String tableStorageConnectionString, String tableName) {

        try {
            return new TableClientBuilder()
                    .connectionString(tableStorageConnectionString)
                    .tableName(tableName)
                    .buildClient();
        } catch (Exception e) {
            LOG.error("Error creating Table Client.",e);
            throw e;
        }
    }


}
