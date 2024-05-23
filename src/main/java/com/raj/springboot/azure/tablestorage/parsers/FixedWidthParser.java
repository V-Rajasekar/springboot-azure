package com.raj.springboot.azure.tablestorage.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FixedWidthParser {
    private static final Logger LOG = LoggerFactory.getLogger(FixedWidthParser.class);
    public String getField(String inputMessage, int startIndex, int length, String topic) {
        if (length != 0) {
            try {
                return inputMessage.substring(startIndex, startIndex + length).trim();
            } catch (Exception ex) {
                LOG.warn("Error parsing the fixed width field for topic: {}; Error:{}", topic, ex.getMessage());
                return null;
            }
        }
        return null;
    }

}
