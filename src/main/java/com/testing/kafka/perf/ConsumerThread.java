package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientPerformance.async;
import static com.testing.kafka.perf.ClientPerformance.currentOffsetMap;
import static com.testing.kafka.perf.ClientPerformance.totalBytesRead;
import static com.testing.kafka.perf.ClientPerformance.totalMessagesRead;
import static com.testing.kafka.perf.ClientPerformance.tps;
import static com.testing.kafka.perf.ConsumerMetric.endOffsetMap;
import static com.testing.kafka.perf.ConsumerMetric.beginOffsetMap;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author euimkks
 */
public class ConsumerThread implements Runnable{
    public static AtomicLong bytesReadKey = new AtomicLong(0);
    public static AtomicLong bytesReadValue = new AtomicLong(0);
    public static AtomicLong bytesRead = new AtomicLong(0);
    public static AtomicLong messagesRead = new AtomicLong(0);
    public static long lastBytesRead = 0;
    public static long lastMessagesRead = 0;
    public static long joinStart = System.currentTimeMillis();
    public static long joinTimeMsInSingleRound = 0;
    public static long ReportTime =0;
    public static ConsumerListener rebalanceListner;
    private KafkaConsumer<String, byte[]> consumer;
    private List<String> topics;
    private ClientUtil util;
    private boolean fullStats;
    private boolean noLogs;
    private long count;
    private int reportingInterval;
    private long testStartTime;
    private CountDownLatch latch;
    private ConsumerMetric consumerMetric;
    private HashMap<TopicPartition,OffsetAndMetadata> commitMap = new HashMap<>();

    ConsumerThread(KafkaConsumer<String, byte[]> consumer,ConsumerMetric consumerMetric,CountDownLatch latch,List<String> topics,ClientUtil util,boolean fullStats,boolean noLogs,long count,int reportingInterval, long testStartTime){
        this.consumer = consumer;
        this.topics = topics;
        this.util = util;
        this.fullStats = fullStats;
        this.noLogs = noLogs;
        this.count = count;
        this.reportingInterval = reportingInterval;
        this.testStartTime = testStartTime;
        this.latch = latch;
        this.consumerMetric = consumerMetric;
    }
 
    @Override
    public void run() {
        try{
            rebalanceListner = new ConsumerListener(joinTimeMsInSingleRound,joinStart,consumer);
            consumer.subscribe(topics, rebalanceListner);
            long currentTimeMillis = System.currentTimeMillis();
            long lastReportTime = currentTimeMillis;
            long lastConsumedTime = currentTimeMillis;
            while (messagesRead.get() < count ) {
                ConsumerRecords<String,byte[]> records = consumer.poll(Duration.ofMillis(1));
                currentTimeMillis = System.currentTimeMillis();
                if (!records.isEmpty()){
                    lastConsumedTime = currentTimeMillis;
                    for (TopicPartition partition : records.partitions()) {
                        List<ConsumerRecord<String, byte[]>> partitionRecords = records.records(partition);
                        for (ConsumerRecord<String,byte[]> record : partitionRecords) {
                            messagesRead.getAndIncrement();
                            if (record.key() != null){
                                bytesReadKey.addAndGet(record.key().length());
                            }
                            if (record.value() != null){
                                bytesReadValue.addAndGet(record.value().length);
                            }
                            bytesRead.set(bytesReadValue.get()+bytesReadKey.get());
                            if (tps.shouldThrottle(messagesRead.get(), currentTimeMillis)) {
                                tps.throttle();
                            }
                            rebalanceListner.addOffset(record.topic(), record.partition(),record.offset());
                            if (currentTimeMillis - lastReportTime >= reportingInterval){
                                if(!noLogs){
                                    consumerMetric.printConsumerProgress(fullStats, bytesRead.get(), lastBytesRead, messagesRead.get(), lastMessagesRead,
                                      lastReportTime, currentTimeMillis, joinTimeMsInSingleRound);
                                }
                                joinTimeMsInSingleRound = 0;
                                lastReportTime = currentTimeMillis;
                                ReportTime = lastReportTime;
                                lastMessagesRead = messagesRead.get();
                                lastBytesRead = bytesRead.get();
                            }
                        }
                        long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                        commitMap.put(partition, new OffsetAndMetadata(lastOffset + 1));
                        if(async.get()){
                            consumer.commitAsync(commitMap,new AsyncCallback(consumer));
                            //consumer.commitAsync(rebalanceListner.getCurrentOffsets(), new AsyncCallback(consumer));
                        }
                        else{
                            consumer.commitSync(commitMap);
                            //consumer.commitSync(rebalanceListner.getCurrentOffsets());
                        }
                    }
                }
            }

            if (messagesRead.get() < count){
                System.out.println("Exiting before consuming the expected number of messages");
            }  
        }
        catch (Exception e) {
            System.out.println("Exception Caught: "+e.getMessage());
            // ignore for shutdown
        } 
        finally {
            latch.countDown();
            System.out.printf("-----------------%s Completed-----------------%n",Thread.currentThread().getName());
            consumer.commitSync(commitMap);
            //consumer.commitSync(rebalanceListner.getCurrentOffsets());
            totalMessagesRead.getAndSet(messagesRead.get());
            totalBytesRead.getAndSet(bytesRead.get());
            Set<TopicPartition> setPartition = consumer.assignment();
            Map<TopicPartition,OffsetAndMetadata> map = consumer.committed(setPartition);
            map.entrySet().forEach(entry -> {
                currentOffsetMap.put(entry.getKey(),entry.getValue().offset());
            });
            endOffsetMap = consumer.endOffsets(setPartition);
            beginOffsetMap = consumer.beginningOffsets(setPartition);
        }        
    }
}
