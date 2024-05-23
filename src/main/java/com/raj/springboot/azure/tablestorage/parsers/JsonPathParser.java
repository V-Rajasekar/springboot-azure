package com.raj.springboot.azure.tablestorage.parsers;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonPathParser {

    private static final Logger LOG = LoggerFactory.getLogger(JsonPathParser.class);

    public String getField(DocumentContext jsonContext, String jsonPath, String topic) {
        try {
            return jsonContext.read(jsonPath);
        } catch (Exception ex) {
            if(!jsonPath.contains("audit")
                && !jsonPath.contains("Uniq")) {
                LOG.warn("Error parsing the json field for topic {}; Error: {}", topic, ex.getMessage());
            }
            return null;
        }
    }

    public DocumentContext getJsonDocument(String jsonString) {
        return JsonPath.parse(jsonString);
    }

}