package com.raj.springboot.azure.tablestorage.parsers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DelimitedFieldParser {
    private static final Logger LOG = LoggerFactory.getLogger(DelimitedFieldParser.class);
    public String getField(String inputMessage, int index, String delimiter, String topic) {
        if (!StringUtils.isEmpty(delimiter)) {
            try {
                return inputMessage.split(delimiter)[index];
            } catch (Exception ex) {
                LOG.warn("Error parsing the delimited field for topic: {}; Error:{}", topic, ex.getMessage());
                return null;
            }
        }
        return null;
    }

}
