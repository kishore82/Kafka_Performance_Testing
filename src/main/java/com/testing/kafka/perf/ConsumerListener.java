package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientPerformance.joinGroupTimeInMs;
import static com.testing.kafka.perf.ClientPerformance.async;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author euimkks
 */
public class ConsumerListener implements ConsumerRebalanceListener{

    private long joinTimeMsInSingleRound;
    private long joinStart;
    private int partition;
    private String topic;
    private KafkaConsumer consumer;
    private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public ConsumerListener(long joinTimeMsInSingleRound, long joinStart, KafkaConsumer consumer) {
        this.joinTimeMsInSingleRound=joinTimeMsInSingleRound;
        this.joinStart = joinStart;
        this.consumer=consumer;
    }

    public void addOffset(String topic, int partition, long offset){
        currentOffsets.put(new TopicPartition(topic,partition),new OffsetAndMetadata(offset,"Commited offset"));
    }
    
    public Map<TopicPartition, OffsetAndMetadata> getCurrentOffsets(){
        return currentOffsets;
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions){
        joinGroupTimeInMs.addAndGet(System.currentTimeMillis() - joinStart);
        joinTimeMsInSingleRound += System.currentTimeMillis() - joinStart;
    }
 
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions){
        joinStart = System.currentTimeMillis();
        if(!async.get()){
            consumer.commitSync(currentOffsets);
            currentOffsets.clear();
        }
    }
}
