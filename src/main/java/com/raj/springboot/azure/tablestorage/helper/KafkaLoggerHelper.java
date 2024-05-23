package com.raj.springboot.azure.tablestorage.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class KafkaLoggerHelper {

   private static final Logger LOG = LoggerFactory.getLogger(KafkaLoggerHelper.class);

   private KafkaLoggerHelper() {}

    public static List<Integer> createAllTableWeekNo
            (final List<String> loadedTables, final String table) {
        List<String>
                loadTableSpec =
                loadedTables.stream().filter(tableDB -> tableDB.startsWith(table)).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(loadTableSpec)) {
            LOG.debug("loaded Specific Table/{}", loadTableSpec);
            return loadTableSpec.stream().map(weekName -> {
                String weekNrStr = weekName.substring(weekName.length() - 2);
                if (!StringUtils.isNumeric(weekNrStr)) {
                    weekNrStr = weekName.substring(weekName.length() - 1);
                }
                if (StringUtils.isNumeric(weekNrStr)) {
                    return Integer.parseInt(weekNrStr);
                }
                return 0;
            }).filter(weekNo -> weekNo != 0).sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static List<Integer> createNonRetentionTableWeekNo( List<Integer> tableWeeks, final int currentWeek,
                                                              final long tableRetentionLimit) {

        List<Integer> distinctTableWeeks = tableWeeks.stream().distinct().collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(distinctTableWeeks)) {
            LOG.info("Table weeks {}", distinctTableWeeks);
            int weeksEndIndex = distinctTableWeeks.size() - 1;
            distinctTableWeeks = distinctTableWeeks.stream().sorted().collect(Collectors.toList());
            int currentPosition = distinctTableWeeks.indexOf(currentWeek);
            if (currentPosition == -1) {
                distinctTableWeeks.add(currentWeek);
                distinctTableWeeks = distinctTableWeeks.stream().sorted().collect(Collectors.toList());
                currentPosition = distinctTableWeeks.indexOf(currentWeek);
            }
            List<Integer> retainWeeks = new ArrayList<>();

            if (tableRetentionLimit > 0) {
                retainWeeks.add(distinctTableWeeks.get(currentPosition));
            }
            int count = 0;
            for (int currIndex = (currentPosition - 1)%distinctTableWeeks.size(); currIndex != currentPosition; currIndex--) {
                if (retainWeeks.size() < tableRetentionLimit) {
                    if (currIndex < 0) {
                        currIndex = weeksEndIndex;
                    }
                    Integer integer = distinctTableWeeks.get(currIndex);
                    retainWeeks.add(integer);
                    count++;
                    continue;
                }
                break;
            }
            return distinctTableWeeks.stream().filter(week -> !retainWeeks.contains(week)).collect(Collectors.toList());
        }
        return null;
    }

    public static List<String> addLeadingZeroInSingleDigit(List<Integer> weeks) {
        if (weeks != null) {
            return (weeks.stream().map(item -> {
                if (item < 10) {
                    return "0" + item;
                } else {
                    return String.valueOf(item);
                }
            }).collect(Collectors.toList()));
        } else {
            return new ArrayList<String>();
        }
    }
}
