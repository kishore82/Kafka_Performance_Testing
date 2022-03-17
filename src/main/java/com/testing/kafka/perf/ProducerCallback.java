package com.testing.kafka.perf;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;


/**
 *
 * @author euimkks
 */
public class ProducerCallback implements Callback {
    private final long start;
    private final int iteration;
    private final int bytes;
    private final ProducerMetric metrics;

    public ProducerCallback(int iter, long start, int bytes, ProducerMetric metrics) {
        this.start = start;
        this.metrics = metrics;
        this.iteration = iter;
        this.bytes = bytes;
    }
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        long now = System.currentTimeMillis();
        int latency = (int) (now - start);
        this.metrics.record(iteration, latency, bytes, now);
        if (exception != null) {
             System.out.println("Exception occured on Producer Callback");
        }
    }
}