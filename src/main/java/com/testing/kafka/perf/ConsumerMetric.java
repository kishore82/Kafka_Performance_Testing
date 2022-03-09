package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientPerformance.joinGroupTimeInMs;
import static com.testing.kafka.perf.ClientPerformance.async;
import static com.testing.kafka.perf.ClientPerformance.totalBytesRead;
import static com.testing.kafka.perf.ClientPerformance.totalMessagesRead;
import static com.testing.kafka.perf.ClientPerformance.tps;
import static com.testing.kafka.perf.ConsumerListener.topicPartition;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

/**
 *
 * @author euimkks
 */
public class ConsumerMetric {
    public static AtomicLong bytesReadKey = new AtomicLong(0);
    public static AtomicLong bytesReadValue = new AtomicLong(0);
    public static AtomicLong bytesRead = new AtomicLong(0);
    public static long messagesRead = 0;
    public static long lastBytesRead = 0;
    public static long lastMessagesRead = 0;
    public static long joinStart = System.currentTimeMillis();
    public static long joinTimeMsInSingleRound = 0;
    public static long ReportTime =0;
    public static ConsumerListener rebalanceListner;
    public static long offset =0;
    
    public void consume(KafkaConsumer<String, byte[]> consumer,
                                List<String> topics,
                                boolean fullStats,
                                boolean noLogs,
                                long count,
                                int reportingInterval,
                                long testStartTime){
        try{
            rebalanceListner = new ConsumerListener(joinTimeMsInSingleRound,joinStart,consumer);
            consumer.subscribe(topics, rebalanceListner);
            long currentTimeMillis = System.currentTimeMillis();
            long lastReportTime = currentTimeMillis;
            long lastConsumedTime = currentTimeMillis;
            long lastOffset = 0;
            while (messagesRead < count ) {
                ConsumerRecords<String,byte[]> records = consumer.poll(Duration.ofMillis(1));
                currentTimeMillis = System.currentTimeMillis();
                if (!records.isEmpty()){
                    lastConsumedTime = currentTimeMillis;
                    for (ConsumerRecord<String,byte[]> record : records) {
                        messagesRead += 1;
                        if (record.key() != null){
                            bytesReadKey.addAndGet(record.key().length());
                        }
                        if (record.value() != null){
                            bytesReadValue.addAndGet(record.value().length);
                        }
                        bytesRead.set(bytesReadValue.get()+bytesReadKey.get());
                        if (tps.shouldThrottle(messagesRead, currentTimeMillis)) {
                            tps.throttle();
                        }
                        rebalanceListner.addOffset(record.topic(), record.partition(),record.offset());
                        if (currentTimeMillis - lastReportTime >= reportingInterval){
                            if(!noLogs){
                                printConsumerProgress(fullStats, bytesRead.get(), lastBytesRead, messagesRead, lastMessagesRead,
                                  lastReportTime, currentTimeMillis, joinTimeMsInSingleRound);
                            }
                            joinTimeMsInSingleRound = 0;
                            lastReportTime = currentTimeMillis;
                            ReportTime = lastReportTime;
                            lastMessagesRead = messagesRead;
                            lastBytesRead = bytesRead.get();
                        }
                    }
                    if(async.get()){
                        consumer.commitAsync(rebalanceListner.getCurrentOffsets(), new AsyncCallback(consumer));
                    }
                    else{
                        consumer.commitSync(rebalanceListner.getCurrentOffsets());
                    }
                }
            }

            if (messagesRead < count){
                System.out.println("Exiting before consuming the expected number of messages");
            }  
        }
        catch (WakeupException e) {
            // ignore for shutdown
        } 
        finally {
            consumer.commitSync(rebalanceListner.getCurrentOffsets());
            totalMessagesRead.getAndSet(messagesRead);
            totalBytesRead.getAndSet(bytesRead.get());
            Set<TopicPartition> set = consumer.assignment();
            Map<TopicPartition,OffsetAndMetadata> map = consumer.committed(set);
            OffsetAndMetadata offsetData = map.get(topicPartition);
            offset = offsetData.offset(); 
        }        
    }

    public void printConsumerProgress(boolean fullStats ,
                          long bytesRead,
                          long lastBytesRead,
                          long messagesRead,
                          long lastMessagesRead,
                          long startMs,
                          long endMs,
                          long periodicJoinTimeInMs){
        printBasicProgress(fullStats,bytesRead, lastBytesRead, messagesRead, lastMessagesRead, startMs, endMs);
        if(fullStats){
            printExtendedProgress(bytesRead, lastBytesRead, messagesRead, lastMessagesRead, startMs, endMs, periodicJoinTimeInMs);
        }
    }

    private void printBasicProgress(boolean fullStats,
                                    long bytesRead,
                                    long lastBytesRead,
                                    long messagesRead,
                                    long lastMessagesRead,
                                    long startMs,
                                    long endMs){
        
        long elapsedMs= (endMs - startMs);
        double totalMbRead = (bytesRead * 1.0) / (1024 * 1024);
        double intervalMbRead = ((bytesRead - lastBytesRead) * 1.0) / (1024 * 1024);
        double intervalMbPerSec = 1000.0 * intervalMbRead / elapsedMs;
        double intervalMessagesPerSec = ((messagesRead - lastMessagesRead) / elapsedMs) * 1000.0;
        if(fullStats){
            System.out.printf("%d records received, %.4f MB, %.4f MB/sec,  %.4f records/sec, ",messagesRead,totalMbRead,
         intervalMbPerSec, intervalMessagesPerSec);
        }
        else{
            System.out.printf("%d records received, %.4f MB, %.4f MB/sec,  %.4f records/sec.%n",messagesRead,totalMbRead,
             intervalMbPerSec, intervalMessagesPerSec);
        }
    }

    private void printExtendedProgress( long bytesRead,
                                        long lastBytesRead,
                                        long messagesRead,
                                       long lastMessagesRead,
                                       long startMs,
                                       long endMs,
                                       long periodicJoinTimeInMs){
        double intervalMbPerSec, intervalMessagesPerSec;
        long fetchTimeMs = Math.abs((endMs - startMs) - periodicJoinTimeInMs);
        double intervalMbRead = ((bytesRead - lastBytesRead) * 1.0) / (1024 * 1024);
        long intervalMessagesRead = messagesRead - lastMessagesRead;
        if (fetchTimeMs <= 0){
          intervalMbPerSec = 0.0;
          intervalMessagesPerSec =0.0;
        }
        else{
          intervalMbPerSec=1000.0 * intervalMbRead / fetchTimeMs;
          intervalMessagesPerSec=1000.0 * intervalMessagesRead / fetchTimeMs;
        }
        System.out.printf("%d rebalance, %d ms fetch time, %.4f fetch MB/sec, %.4f fetch records/sec.%n",periodicJoinTimeInMs, fetchTimeMs, intervalMbPerSec, intervalMessagesPerSec);
    }
    
    public void summary(double elapsedSecs,long fetchTimeInMs ,double totalMBRead){
        System.out.println("--------Consumer Performance Summary Report--------\n");
        System.out.printf("Count of total record received: %d \n", totalMessagesRead.get());
        System.out.printf("Total MB received:  %.2f MB\n", totalMBRead);
        System.out.printf("Records per second received:  %.2f records/sec\n", totalMessagesRead.get() / elapsedSecs);
        System.out.printf("Mb per second:  %.2f MB/sec\n" , totalMBRead / elapsedSecs);
        System.out.println("Offset committed: "+offset);
        System.out.printf("Rebalance Time in ms:  %d ms \n", joinGroupTimeInMs.get());
        System.out.printf("Fetch Time in ms:  %d ms \n", fetchTimeInMs);
        System.out.printf("Fetch Mb per second:  %.2f MB/sec \n", totalMBRead / (fetchTimeInMs / 1000.0));
        System.out.printf("Fetch records per second:  %.2f records/sec \n", totalMessagesRead.get() / (fetchTimeInMs / 1000.0));
        System.out.println("\n--------------End Of Summary Report----------------");
    }
}
