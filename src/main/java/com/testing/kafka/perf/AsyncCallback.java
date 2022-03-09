package com.testing.kafka.perf;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author euimkks
 */
public class AsyncCallback implements OffsetCommitCallback {
    private AtomicLong position = new AtomicLong(0);
    public KafkaConsumer<String,byte[]> consumer;
    
    public AsyncCallback(KafkaConsumer<String,byte[]> consumer){
        this.consumer = consumer;
    }

    @Override
    public void onComplete(Map<TopicPartition, OffsetAndMetadata> map, Exception ex){
        position.incrementAndGet();
        if(ex!= null){
            System.out.println("Commit failed for offsets {}"+ map.toString()+" with exception:  "+ex);
            System.out.println("position value: "+position.get());
        }
    }
}
