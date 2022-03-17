package com.testing.kafka.perf;

import java.util.Map;
import java.util.TreeMap;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

/**
 *
 * @author euimkks
 */
public class MetricsUtil {
    public static void printMetrics(Map<MetricName, ? extends Metric> metrics) {
        if (metrics != null && !metrics.isEmpty()) {
            int maxLengthOfDisplayName = 0;
            TreeMap<String, Object> sortedMetrics = new TreeMap<>();
            for (Metric metric : metrics.values()) {
                MetricName mName = metric.metricName();
                String mergedName = mName.group() + ":" + mName.name() + ":" + mName.tags();
                maxLengthOfDisplayName = maxLengthOfDisplayName < mergedName.length() ? mergedName.length() : maxLengthOfDisplayName;
                sortedMetrics.put(mergedName, metric.metricValue());
            }
            String doubleOutputFormat = "%-" + maxLengthOfDisplayName + "s : %.3f";
            String defaultOutputFormat = "%-" + maxLengthOfDisplayName + "s : %s";
            System.out.println(String.format("\n%-" + maxLengthOfDisplayName + "s   %s", "Metric Name", "Value"));

            sortedMetrics.entrySet().forEach(entry -> {
                String outputFormat;
                if (entry.getValue() instanceof Double)
                    outputFormat = doubleOutputFormat;
                else
                    outputFormat = defaultOutputFormat;
                System.out.println(String.format(outputFormat, entry.getKey(), entry.getValue()));
            });
        }
    }
}
