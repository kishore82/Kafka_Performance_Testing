package com.testing.kafka.perf;

/**
 *
 * @author euimkks
 */
public class Throughput implements Runnable{
    
    private static final long NS_PER_MS = 1000000L;
    private static final long NS_PER_SEC = 1000 * NS_PER_MS;
    private static final long MIN_SLEEP_NS = 2 * NS_PER_MS;

    private final long startMs;
    private final long sleepTimeNs;
    private final long targetThroughput;

    private long sleepDeficitNs = 0;
    private boolean wakeup = false;

    public Throughput(long targetThroughput, long startMs) {
        this.startMs = startMs;
        this.targetThroughput = targetThroughput;
        this.sleepTimeNs = targetThroughput > 0 ? NS_PER_SEC / targetThroughput :  Long.MAX_VALUE;
    }
	
    public boolean shouldThrottle(long amountSoFar, long sendStartMs) {
        if (this.targetThroughput < 0) {
            return false;
        }

        float elapsedSec = (sendStartMs - startMs) / 1000.f;
        return elapsedSec > 0 && (amountSoFar / elapsedSec) > this.targetThroughput;
    }
    @Override
    public void run(){}
    public void throttle() {
        if (targetThroughput == 0) {
            try {
                synchronized (this) {
                    while (!wakeup) {
                        this.wait();
                    }
                }
            } catch (InterruptedException e) {
                 System.out.println("Exception occured: "+e.getMessage());
            }
        }

        sleepDeficitNs += sleepTimeNs;
		
        if (sleepDeficitNs >= MIN_SLEEP_NS) {
            long sleepStartNs = System.nanoTime();
            try {
                synchronized (this) {
                    long remaining = sleepDeficitNs;
                    while (!wakeup && remaining > 0) {
                        long sleepMs = remaining / 1000000;
                        long sleepNs = remaining - sleepMs * 1000000;
                        this.wait(sleepMs, (int) sleepNs);
                        long elapsed = System.nanoTime() - sleepStartNs;
                        remaining = sleepDeficitNs - elapsed;
                    }
                    wakeup = false;
                }
                sleepDeficitNs = 0;
            } catch (InterruptedException e) {
                long sleepElapsedNs = System.nanoTime() - sleepStartNs;
                if (sleepElapsedNs <= sleepDeficitNs) {
                    sleepDeficitNs -= sleepElapsedNs;
                }
            }
        }
    }

    public void wakeup() {
        synchronized (this) {
            wakeup = true;
            this.notifyAll();
        }
    }
}
