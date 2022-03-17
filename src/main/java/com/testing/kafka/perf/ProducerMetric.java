package com.testing.kafka.perf;

import org.apache.kafka.clients.producer.Callback;

public class ProducerMetric implements Runnable{
    private long start;
    private long windowStart;
    private int[] latencies;
    private int sampling;
    private int iteration;
    private int index;
    private long count;
    private long bytes;
    private int maxLatency;
    private long totalLatency;
    private long windowCount;
    private int windowMaxLatency;
    private long windowTotalLatency;
    private long windowBytes;
    private long reportingInterval;
    private int totalCount;
    private long elapsed;
    private double recsPerSec;
    private double mbPerSec;
    private double mb;

    public ProducerMetric(long numRecords, int reportingInterval) {
        this.start = System.currentTimeMillis();
        this.windowStart = System.currentTimeMillis();
        this.iteration = 0;
        this.sampling = (int) (numRecords / Math.min(numRecords, 500000));
        this.latencies = new int[(int) (numRecords / this.sampling) + 1];
        this.index = 0;
        this.maxLatency = 0;
        this.totalLatency = 0;
        this.windowCount = 0;
        this.windowMaxLatency = 0;
        this.windowTotalLatency = 0;
        this.windowBytes = 0;
        this.totalLatency = 0;
        this.reportingInterval = reportingInterval;
        this.totalCount=0;
    }
    @Override
    public void run() {}
    public void record(int iter, int latency, int bytes, long time) {
        this.count++;
        this.bytes += bytes;
        this.totalLatency += latency;
        this.maxLatency = Math.max(this.maxLatency, latency);
        this.windowCount++;
        this.windowBytes += bytes;
        this.windowTotalLatency += latency;
        this.windowMaxLatency = Math.max(windowMaxLatency, latency);
        if (iter % this.sampling == 0) {
            this.latencies[index] = latency;
            this.index++;
        }

        if (time - windowStart >= reportingInterval) {
            totalCount +=windowCount;
            printWindow();
            newWindow();
        }
    }

    public Callback nextCompletion(long start, int bytes, ProducerMetric metrics) {
        Callback cb = new ProducerCallback(this.iteration, start, bytes, metrics);
        this.iteration++;
        return cb;
    }

    public void printWindow() {
        long diff = System.currentTimeMillis() - windowStart;
        double recsSec = 1000.0 * windowCount / (double) diff;
        double mbSec = 1000.0 * this.windowBytes / (double) diff / (1024.0 * 1024.0);
        double mbWindow = (1.0 * this.windowBytes) / (1024.0 * 1024.0);
        System.out.printf("%d records sent, %.2f MB, %.1f records/sec (%.2f MB/sec), %.1f ms avg latency, %.1f ms max latency, %d total records sent.%n",
                          windowCount,
                          mbWindow,
                          recsSec,
                          mbSec,
                          windowTotalLatency / (double) windowCount,
                          (double) windowMaxLatency,
                          totalCount);
    }

    public void newWindow() {
        this.windowStart = System.currentTimeMillis();
        this.windowCount = 0;
        this.windowMaxLatency = 0;
        this.windowTotalLatency = 0;
        this.windowBytes = 0;
    }

    public void printLast() {
        elapsed = System.currentTimeMillis() - start;
        recsPerSec = 1000.0 * count / (double) elapsed;
        mbPerSec = 1000.0 * this.bytes / (double) elapsed / (1024.0 * 1024.0);
        mb = (this.bytes * 1.0) / (1024.0 * 1024.0);
        double recsSec = 1000.0 * windowCount / (double) elapsed;
        double mbSec = 1000.0 * this.windowBytes / (double) elapsed / (1024.0 * 1024.0);
        double MB = (1.0 * this.windowBytes) / (1024.0 * 1024.0);
        System.out.printf("%d records sent, %.2f MB, %.1f records/sec (%.2f MB/sec), %.1f ms avg latency, %.1f ms max latency, %d total records sent.%n",
                          windowCount,
                          MB,
                          recsSec,
                          mbSec,
                          windowTotalLatency / (double) windowCount,
                          (double) windowMaxLatency,
                          totalCount+=windowCount);
    }

    public void summary(){
        System.out.println("--------Producer Performance Summary Report--------\n");
        System.out.printf("Count of total record sent: %d \n", count);
        System.out.printf("Total MB sent:  %.2f MB\n",mb);
        System.out.printf("Records per second sent:  %.2f records/sec\n", recsPerSec);
        System.out.printf("Mb per second:  %.2f MB/sec\n" , mbPerSec);
        System.out.printf("Average latency in ms:  %.2f ms \n", totalLatency / (double) count);
        System.out.printf("Maximum latency in ms:  %.2f ms \n", (double) maxLatency);
        System.out.println("\n--------------End Of Summary Report----------------");
    } 
}