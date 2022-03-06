package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientPerformance.joinGroupTimeInMs;
import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author euimkks
 */
public class ConsumerListener implements ConsumerRebalanceListener{

    private long joinTimeMsInSingleRound;
    private long joinStart;

    public ConsumerListener(long joinTimeMsInSingleRound, long joinStart) {
        this.joinTimeMsInSingleRound=joinTimeMsInSingleRound;
        this.joinStart = joinStart;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions){
            joinGroupTimeInMs.addAndGet(System.currentTimeMillis() - joinStart);
            joinTimeMsInSingleRound += System.currentTimeMillis() - joinStart;
    }
 
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions){
      joinStart = System.currentTimeMillis();
    }
}
