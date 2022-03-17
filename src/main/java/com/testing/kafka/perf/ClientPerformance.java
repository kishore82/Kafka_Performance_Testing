package com.testing.kafka.perf;

import static com.testing.kafka.perf.ClientUtil.topicList;
import static com.testing.kafka.perf.ConsumerThread.ReportTime;
import static com.testing.kafka.perf.ConsumerThread.bytesRead;
import static com.testing.kafka.perf.ConsumerThread.joinTimeMsInSingleRound;
import static com.testing.kafka.perf.ConsumerThread.lastBytesRead;
import static com.testing.kafka.perf.ConsumerThread.lastMessagesRead;
import static com.testing.kafka.perf.ConsumerThread.messagesRead;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import static net.sourceforge.argparse4j.impl.Arguments.store;
import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import org.apache.kafka.clients.producer.KafkaProducer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Exit;

public class ClientPerformance { 
    public static AtomicLong totalMessagesRead = new AtomicLong(0);
    public static AtomicLong totalBytesRead = new AtomicLong(0);
    public static AtomicLong joinGroupTimeInMs = new AtomicLong(0);
    public static double elapsedSecs = 0;
    public static long fetchTimeInMs = 0;
    public static double totalMBRead = 0;
    public static Throughput tps;
    public static AtomicBoolean async = new AtomicBoolean(false);
    public static HashMap<TopicPartition, Long> currentOffsetMap	 = new HashMap<>();
    public static void main(String[] args) throws Exception {
        byte[] payload =null;
        ArgumentParser parser = argParser();
        Properties adminProps = new Properties();
        TopicPartition topicPartition;
        KafkaProducer<String, byte[]> producer ;
        KafkaConsumer <String, byte[]> consumer;
        Map<MetricName, ? extends Metric> metrics=null;
        SimpleDateFormat dateformat = new SimpleDateFormat("hh:mm:ss a dd-MMM-yyyy");  
        final Thread mainThread = Thread.currentThread();
        try {
            Namespace res = parser.parseArgs(args);
            String clientConfig = res.getString("clientConfig");
            int throughput = res.getInt("throughput");
            int interval = res.getInt("interval");
            boolean printMetrics = res.getBoolean("printMetrics");
            if (clientConfig.isEmpty()) {
                throw new ArgumentParserException("Common Properties must not be empty.", parser);
            }
            ClientUtil util = new ClientUtil(clientConfig);
            util.getCommonProperties(adminProps);
            CountDownLatch latch; 
            System.out.println("Topic properties: "+adminProps.toString()+"\n");
            if(res.get("type").equals("produce")){
                long numRecords = res.getLong("numRecords");
                Integer threadCount = res.getInt("threadCount");
                Integer recordSize = res.getInt("recordSize");
                String payloadFile = res.getString("payloadFile");
                if (numRecords == 0) {
                    throw new ArgumentParserException("--num-records must be greater than zero.", parser);
                }
                String payloadDelimiter = res.getString("payloadDelimiter").equals("\\n") ? "\n" : res.getString("payloadDelimiter");
                List<byte[]> payloadList = util.readPayloadFromFile(payloadFile, payloadDelimiter);
                if(recordSize!=null){
                    if(recordSize==0){
                        throw new ArgumentParserException("--recordSize must be greater than zero.", parser);
                    }
                    payload = util.generateRandomByte(recordSize);
                }
                Properties producerProps = util.getProducerProps();
                System.out.println("Producer properties: "+producerProps.toString()+"\n");
                producer = new KafkaProducer<>(producerProps);   
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        System.out.println("Shutting down producer.....");
                        producer.flush();
                        producer.close();
                        try {
                            mainThread.join();
                        } catch (InterruptedException e) {
                            System.out.println("Exception occured in producer shutdown thread: "+e.getMessage());
                        }
                    }
                });  
                latch  = new CountDownLatch(threadCount); 
                ProducerMetric metric = new ProducerMetric(numRecords*topicList.size()*threadCount, interval);
                long startMs = System.currentTimeMillis();
                System.out.println("-----------------Starting Producer-----------------");
                Thread[] producerThreads = new Thread[threadCount];
                try{
                    for(int i =0;i<producerThreads.length;i++){
                        producerThreads[i]=new Thread(new ProducerThread(producer, latch,payloadList,payload, numRecords, payloadFile, metric, new Throughput(throughput, startMs)),"ProducerThread-"+i);
                        producerThreads[i].start();
                        producerThreads[i].join();
                        startMs = System.currentTimeMillis();
                    }  
                }
                catch (InterruptedException e){
                    System.out.println("Thread Interrupted: "+e.getMessage());
                }
                latch.await();
                producer.flush();
                metric.printLast();
                System.out.println("Traffic is stopping now....\n");
                System.out.println("Started at: "+dateformat.format(new Date(startMs))+"\n");
                System.out.println("Stopped at: "+dateformat.format(new Date().getTime())+"\n");
                metric.summary();
                if(printMetrics){
                    metrics = producer.metrics();               
                }
                producer.close();
                if(metrics!=null){
                    MetricsUtil.printMetrics(metrics); 
                }
            }
            else{
                long receiveSize = res.getLong("receiveSize");
                boolean noLogs = res.getBoolean("logs");
                boolean fullStats = res.getBoolean("fullStats");
                boolean asyncCommit = res.getBoolean("async");
                async.getAndSet(asyncCommit);
                if(receiveSize<=0){
                    throw new ArgumentParserException("Consumer properties must be greater than zero and not null.", parser);
                }
                Properties consumerProps = util.getConsumerProps();
                System.out.println("Consumer Properties: "+consumerProps.toString()+"\n");
                long startMs, endMs = 0;
                consumer = new KafkaConsumer<>(consumerProps);
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        System.out.println("Shutting down consumer.....");
                        consumer.wakeup();
                        try {
                            mainThread.join();
                        } catch (InterruptedException e) {
                            System.out.println("Exception occured in consumer shutdown thread: "+e.getMessage());
                        }
                    }
                });
                latch  = new CountDownLatch(1);
                startMs = System.currentTimeMillis();
                ConsumerMetric consumermetric = new ConsumerMetric();
                tps = new Throughput(throughput, startMs);
                System.out.println("-----------------Starting Consumer-----------------");
                try{
                    Thread consumerThread =new Thread(new ConsumerThread(consumer, consumermetric,latch,topicList,util,fullStats,noLogs,receiveSize, interval, startMs),"ConsumerThread");
                    consumerThread.start();
                    consumerThread.join();
                }
                catch (InterruptedException e){
                    System.out.println("Thread Interrupted: "+e.getMessage());
                }
                latch.await();
                endMs = System.currentTimeMillis();
                if(printMetrics){
                    metrics = consumer.metrics();               
                }
                consumer.close();
                elapsedSecs = (endMs - startMs) / 1000.0;
                fetchTimeInMs = Math.abs((endMs - startMs) - joinGroupTimeInMs.get());
                totalMBRead = (totalBytesRead.get() * 1.0) / (1024 * 1024);
                if(noLogs && fullStats){
                    System.out.printf("%d records received, %.4f MB, %.4f MB/sec,  %.4f records/sec, %d rebalance, %d ms fetch time, %.4f fetch MB/sec, %.4f fetch records/sec. %n",
                        totalMessagesRead.get(),
                        totalMBRead,
                        totalMBRead / elapsedSecs,
                        totalMessagesRead.get() / elapsedSecs,
                        joinGroupTimeInMs.get(),
                        fetchTimeInMs,
                        totalMBRead / (fetchTimeInMs / 1000.0),
                        totalMessagesRead.get() / (fetchTimeInMs / 1000.0)
                    );
                }      
                if(noLogs && !fullStats){
                    System.out.printf("%d records received, %.4f MB, %.4f MB/sec,  %.4f records/sec. %n",
                        totalMessagesRead.get(),
                        totalMBRead,
                        totalMBRead / elapsedSecs,
                        totalMessagesRead.get() / elapsedSecs
                    );
                }
                if(!noLogs){
                    consumermetric.printConsumerProgress(fullStats, bytesRead.get(), lastBytesRead, messagesRead.get(), lastMessagesRead,
                              ReportTime, System.currentTimeMillis(), joinTimeMsInSingleRound);
                }
                System.out.println("Traffic is stopping now....\n");
                System.out.println("Started at: "+dateformat.format(new Date(startMs))+"\n");
                System.out.println("Stopped at: "+dateformat.format(new Date().getTime())+"\n");
                consumermetric.summary(elapsedSecs,fetchTimeInMs,totalMBRead);
                if(metrics!=null){
                    MetricsUtil.printMetrics(metrics); 
                }
            } 
        }
        catch (ArgumentParserException e ) {
            if (args.length == 0) {
                parser.printHelp();
                Exit.exit(0);
            }
            else {
                parser.handleError(e);
                Exit.exit(1);
            }
        }
    }

    private static ArgumentParser argParser() {
        ArgumentParser parser = ArgumentParsers
                .newArgumentParser("client-performance")
                .defaultHelp(true)
                .description("This tool is used to verify the client performance.");
        
        Subparsers subparsers = parser.addSubparsers()
                .dest("type")
                .help("Choose produce or consume")
                .description("Based on the subcommand, choose the argument");

        Subparser produce = subparsers
                .addParser("produce")
                .defaultHelp(true)
                .description("Choose the below named arguments for producing");
        
        Subparser consume = subparsers
                .addParser("consume")
                .defaultHelp(true)
                .description("Choose the below named arguments for consuming");
        
        MutuallyExclusiveGroup payloadOptions = produce
                .addMutuallyExclusiveGroup()
                .required(true)
                .description("Either --record-size or --payload-file must be specified but not both.");

        produce.addArgument("--num-record")
                .action(store())
                .required(true)
                .type(Long.class)
                .metavar("NUM-RECORDS")
                .dest("numRecords")
                .help("Number of messages to produce.");

        parser.addArgument("--throughput")
                .action(store())
                .required(true)
                .type(Integer.class)
                .metavar("THROUGHPUT")
                .dest("throughput")
                .help("Set this to -1 to disable throttling.");
 
        produce.addArgument("--threadCount")
                .action(store())
                .required(true)
                .type(Integer.class)
                .metavar("THREAD COUNT")
                .dest("threadCount")
                .help("Number of threads.");

        consume.addArgument("--size")
                .action(store())
                .required(true)
                .type(Long.class)
                .metavar("RECEIVE-SIZE")
                .dest("receiveSize")
                .help("Number of messages to consume.");

        parser.addArgument("--interval")
                .action(store())
                .required(false)
                .setDefault(2000)
                .type(Integer.class)
                .metavar("REPORTING-INTERVAL-TIME")
                .dest("interval")
                .help("Interval in ms at which to print progress info.");
         
        consume.addArgument("--no-logs")
                .action(storeTrue())
                .type(Boolean.class)
                .required(false)
                .metavar("NO-INTERMEDIATE-LOGS")
                .dest("logs")
                .help("Consume without printing logs.");

        consume.addArgument("--async")
                .action(storeTrue())
                .type(Boolean.class)
                .required(false)
                .metavar("COMMIT-ASYNC")
                .dest("async")
                .help("Commit offsets asynchronously.");
 
        consume.addArgument("--full")
                .action(storeTrue())
                .type(Boolean.class)
                .required(false)
                .metavar("COMPLETE-STATS")
                .dest("fullStats")
                .help("Print complete stats while consuming.");

        payloadOptions.addArgument("--size")
                .action(store())
                .required(false)
                .type(Integer.class)
                .metavar("RECORD-SIZE")
                .dest("recordSize")
                .help("Message size in bytes.");

        payloadOptions.addArgument("--payload-file")
                .action(store())
                .required(false)
                .type(String.class)
                .metavar("PAYLOAD-FILE")
                .dest("payloadFile")
                .help("Payload for message to be sent. This works only for UTF-8 encoded text files. ");

        produce.addArgument("--payload-delimiter")
                .action(store())
                .required(false)
                .type(String.class)
                .metavar("PAYLOAD-DELIMITER")
                .dest("payloadDelimiter")
                .setDefault("\\n")
                .help("Defaults to new line. " +
                      "This parameter will be ignored if --payload-file is not provided.");
        
        parser.addArgument("--print-metrics")
                .action(storeTrue())
                .type(Boolean.class)
                .required(false)
                .metavar("PRINT-METRICS")
                .dest("printMetrics")
                .help("Print out metrics at the end of the test.");
        
        parser.addArgument("--client.config")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("CLIENT-CONFIG-FILE")
                .dest("clientConfig")
                .help("Client config properties file containing both ssl, producer and consumer properties.");
        
        return parser;
    }
}