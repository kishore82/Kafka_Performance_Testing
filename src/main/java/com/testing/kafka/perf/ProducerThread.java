package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientUtil.topicList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author euimkks
 */
public class ProducerThread implements Runnable {
    private KafkaProducer<String, byte[]> producer;
    private List<byte[]> payloadList;
    private byte[] payload;
    private ProducerRecord<String, byte[]> record;
    private long numRecords;
    private ProducerMetric metric;
    private Throughput tps;
    private String payloadFile;
    private Random random = new Random(0);
    private CountDownLatch latch;
    public static long currentTime;

    ProducerThread(KafkaProducer<String, byte[]> producer, CountDownLatch latch,List<byte[]> payloadList,byte[] payload, long numRecords,String payloadFile,ProducerMetric metric,Throughput tps) {
        this.producer = producer;
        this.payloadList = payloadList;
        this.payload = payload;
        this.numRecords = numRecords;
        this.metric = metric;
        this.tps = tps;
        this.payloadFile =  payloadFile;
        this.latch = latch;
    }

    @Override
    public void run() {
        try{
            topicList.forEach(topicName -> {
                for (long i = 0; i < numRecords; i++) {
                    if (payloadFile != null && !payloadFile.isEmpty()) {
                        payload = payloadList.get(random.nextInt(payloadList.size()));
                    }
                    record = new ProducerRecord<>(topicName, payload);
                    long sendStartMs = System.currentTimeMillis();
                    org.apache.kafka.clients.producer.Callback cb = metric.nextCompletion(sendStartMs, payload.length, metric);
                    producer.send(record, cb);

                    if (tps.shouldThrottle(i, sendStartMs)) {
                        tps.throttle();
                    }
                }
            });
        }
        catch (Exception e ) {
            System.out.println("Exception caught: "+e.getMessage());
            // ignore for shutdown
        }
        finally{
            latch.countDown();
            currentTime = System.currentTimeMillis();
            System.out.printf("-----------------%s Completed-----------------%n",Thread.currentThread().getName());
        }
    }
}
