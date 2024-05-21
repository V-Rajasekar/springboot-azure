package com.raj.springboot.azure.cosmos.config;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azure.spring.data.cosmos.core.ResponseDiagnostics;
import com.azure.spring.data.cosmos.core.ResponseDiagnosticsProcessor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.annotation.Nullable;

@Component
public class CosmosResponseDiagnosticsMetricsReporter implements ResponseDiagnosticsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CosmosResponseDiagnosticsMetricsReporter.class);

    private static final Pattern REQUEST_CHARGE_PATTERN = Pattern.compile("\\\"requestCharge\\\"\\:(\\d+.\\d*),");

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\\"requestOperationType\\\"\\:\\\"(\\w+)\\\"");

    private static final String COSMOS_OPERATIONS_COUNT_METRIC = "cosmos.operations.count";

    private static final String COSMOS_RU_CONSUMED_SUM_METRIC = "cosmos.ru.consumed.sum";

    private static final String COSMOS_OPERATION_TAG_NAME = "operation";

    private static final String EMPTY_TAG_VALUE = "";

    private final MeterRegistry meterRegistry;

    private final Double diagnosticLoggingThreshold;

    public CosmosResponseDiagnosticsMetricsReporter(MeterRegistry meterRegistry, @Value("${spring.cosmos" +
            ".diagnosticLoggingThreshold:100}") Double diagnosticLoggingThreshold) {
        super();
        this.meterRegistry = meterRegistry;
        this.diagnosticLoggingThreshold = diagnosticLoggingThreshold;
    }

    @Override
    public void processResponseDiagnostics(@Nullable ResponseDiagnostics responseDiagnostics) {
        if (responseDiagnostics != null) {
            Double totalRequestCharge = 0.0;
            if (responseDiagnostics.getCosmosResponseStatistics() != null) {
                totalRequestCharge = responseDiagnostics.getCosmosResponseStatistics().getRequestCharge();
            }
            if (responseDiagnostics.getCosmosDiagnostics() != null) {
                String responseDiagnosticsString = responseDiagnostics.toString();
                if (totalRequestCharge == 0.0) {
                    totalRequestCharge = parseRUFromCosmosDiagnostics(responseDiagnosticsString);
                }
                CosmosTags cosmosTags = new CosmosTags(responseDiagnosticsString);
                incrementCounter(COSMOS_OPERATIONS_COUNT_METRIC, 1.0, cosmosTags.getTags());
                incrementCounter(COSMOS_RU_CONSUMED_SUM_METRIC, totalRequestCharge, cosmosTags.getTags());
                LOG.debug("Cosmos RU consumed: {} for {} operation", totalRequestCharge, cosmosTags.getOperation());
                if (totalRequestCharge > this.diagnosticLoggingThreshold) {
                    LOG.debug("Cosmos Response Diagnostics {}", responseDiagnostics);
                }
            }
        }
    }

    private void incrementCounter(String counterName, Double incrementValue, String... tagKeyValue) {
        try {
            Counter counter = meterRegistry.find(counterName).tags(tagKeyValue).counter();
            if (counter == null) {
                counter = Counter.builder(counterName).tags(tagKeyValue).register(meterRegistry);
            }
            counter.increment(incrementValue);
        } catch (Exception ex) {
            LOG.debug("Error while incrementing Cosmos Metric {} with tags {}", counterName,
                    Arrays.toString(tagKeyValue));
        }
    }

    private Double parseRUFromCosmosDiagnostics(String responseDiagnostics) {
        Double totalRequestCharge = 0.0;
        try {
            Matcher requestChargeMatcher = REQUEST_CHARGE_PATTERN.matcher(responseDiagnostics);
            while (requestChargeMatcher.find()) {
                totalRequestCharge = totalRequestCharge + Double.parseDouble(requestChargeMatcher.group(1));
            }
        } catch (Exception e) {
            LOG.debug("Error parsing the Cosmos Diagnostics for RUs");
        }
        return totalRequestCharge;
    }

    private static class CosmosTags {
        String operation = EMPTY_TAG_VALUE;

        String[] tags = new String[2];

        CosmosTags(String responseDiagnostics) {
            try {
                Matcher operationMatcher = OPERATION_PATTERN.matcher(responseDiagnostics);
                if (operationMatcher.find()) {
                    this.operation = operationMatcher.group(1);
                }
            } catch (Exception e) {
                LOG.debug("Error parsing the Cosmos Diagnostics for Cosmos tags");
            }
            setTags();
        }

        void setTags() {
            tags[0] = COSMOS_OPERATION_TAG_NAME;
            tags[1] = this.operation;
        }

        String[] getTags() {
            return tags;
        }

        String getOperation() {
            return operation;
        }
    }
}
