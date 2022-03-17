package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientPerformance.currentOffsetMap;
import static com.testing.kafka.perf.ClientPerformance.joinGroupTimeInMs;
import static com.testing.kafka.perf.ClientPerformance.totalMessagesRead;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;

/**
 *
 * @author euimkks
 */
public class ConsumerMetric implements Runnable{
    public static Map<TopicPartition,Long> endOffsetMap;
    public static Map<TopicPartition,Long> beginOffsetMap;
    ConsumerMetric(){
        
    }
   
    @Override
    public void run() {
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
        System.out.printf("Rebalance Time in ms:  %d ms \n", joinGroupTimeInMs.get());
        System.out.printf("Fetch Time in ms:  %d ms \n", fetchTimeInMs);
        System.out.printf("Fetch Mb per second:  %.2f MB/sec \n", totalMBRead / (fetchTimeInMs / 1000.0));
        System.out.printf("Fetch records per second:  %.2f records/sec \n", totalMessagesRead.get() / (fetchTimeInMs / 1000.0));
        System.out.println("\n--------------End Of Summary Report----------------\n");
        System.out.println("\n-------------Consumer Offset Management------------\n");
        try{
            String begin = new ObjectMapper().writeValueAsString(beginOffsetMap);
            String current = new ObjectMapper().writeValueAsString(currentOffsetMap);
            String end = new ObjectMapper().writeValueAsString(endOffsetMap);
            System.out.println("Beginning offsets: "+begin);
            System.out.println("Current offsets: "+current);
            System.out.println("Ending offsets: "+end);
        }
        catch(JsonProcessingException e ){
            System.out.println("Exception occured while parsing json: "+e.getMessage());
        }
        System.out.println("\n----------------End of Offset Report---------------\n");
    }
}