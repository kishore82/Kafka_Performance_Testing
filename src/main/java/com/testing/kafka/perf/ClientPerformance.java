package com.testing.kafka.perf;

import static com.testing.kafka.perf.ConsumerMetric.ReportTime;
import static com.testing.kafka.perf.ConsumerMetric.bytesRead;
import static com.testing.kafka.perf.ConsumerMetric.joinTimeMsInSingleRound;
import static com.testing.kafka.perf.ConsumerMetric.lastBytesRead;
import static com.testing.kafka.perf.ConsumerMetric.lastMessagesRead;
import static com.testing.kafka.perf.ConsumerMetric.messagesRead;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import static net.sourceforge.argparse4j.impl.Arguments.store;
import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.utils.Exit;

public class ClientPerformance { 
    public static AtomicLong totalMessagesRead = new AtomicLong(0);
    public static AtomicLong totalBytesRead = new AtomicLong(0);
    public static AtomicLong joinGroupTimeInMs = new AtomicLong(0);
    public static double elapsedSecs = 0;
    public static long fetchTimeInMs = 0;
    public static double totalMBRead = 0;
    public static void main(String[] args) throws Exception {
        byte[] payload =null;
        ArgumentParser parser = argParser();
        Properties adminProps = new Properties();
        Random random = new Random(0);
        KafkaProducer<String, byte[]> producer ;
        KafkaConsumer <String, byte[]> consumer;
        SimpleDateFormat dateformat = new SimpleDateFormat("hh:mm:ss a dd-MMM-yyyy");  
        try {
            Namespace res = parser.parseArgs(args);
            String topicName = res.getString("topic-name");
            String clientConfig = res.getString("clientConfig");
            String bootstrapServer = res.getString("server");
            boolean printMetrics = res.getBoolean("printMetrics");
            if (clientConfig.isEmpty() || topicName.isEmpty() || bootstrapServer.isEmpty()) {
                throw new ArgumentParserException("Common Properties must not be empty.", parser);
            }
            ClientUtil util = new ClientUtil(clientConfig);
            util.getCommonProperties(adminProps);
            adminProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
            System.out.println("Topic properties: "+adminProps.toString()+"\n");
            if(res.get("type").equals("produce")){
                long numRecords = res.getLong("numRecords");
                Integer recordSize = res.getInt("recordSize");
                String payloadFile = res.getString("payloadFile");
                int throughput = res.getInt("throughput");
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
                producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
                System.out.println("Producer properties: "+producerProps.toString()+"\n");
                producer = new KafkaProducer<>(producerProps);                
                ProducerRecord<String, byte[]> record;
                ProducerMetric metric = new ProducerMetric(numRecords, 5000);
                long startMs = System.currentTimeMillis();
                Throughput tps = new Throughput(throughput, startMs);
                System.out.println("-----------------Starting Producer-----------------");
                for (long i = 0; i < numRecords; i++) {
                    if (payloadFile != null && !payloadFile.isEmpty()) {
                        payload = payloadList.get(random.nextInt(payloadList.size()));
                    }
                    record = new ProducerRecord<>(topicName, payload);
                    long sendStartMs = System.currentTimeMillis();
                    Callback cb = metric.nextCompletion(sendStartMs, payload.length, metric);
                    producer.send(record, cb);
                    
                    if (tps.shouldThrottle(i, sendStartMs)) {
                        tps.throttle();
                    }
                }
                producer.flush();
                metric.printLast();
                System.out.println("Traffic is stopping now....\n");
                System.out.println("Started at: "+dateformat.format(new Date(startMs))+"\n");
                System.out.println("Stopped at: "+dateformat.format(new Date().getTime())+"\n");
                metric.summary();
                if(printMetrics){
                MetricsUtil.printMetrics(producer.metrics());                
                }
                producer.close();
            }
            else{
                Map<MetricName, ? extends Metric> metrics=null;
                long receiveSize = res.getLong("receiveSize");
                String groupID = res.getString("groupID");
                int reportingInterval = res.getInt("reportingInterval");
                boolean detailedStats = res.getBoolean("detailedStats");
                boolean fullStats = res.getBoolean("fullStats");
                if(receiveSize<=0||groupID.isEmpty()){
                    throw new ArgumentParserException("Consumer properties must be greater than zero and not null.", parser);
                }
                Properties consumerProps = util.getConsumerProps();
                consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
                consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,groupID);
                System.out.println("Consumer Properties: "+consumerProps.toString()+"\n");
                ConsumerMetric consumermetric = new ConsumerMetric();
                long startMs, endMs = 0;
                consumer = new KafkaConsumer<>(consumerProps);
                startMs = System.currentTimeMillis();
                System.out.println("-----------------Starting Consumer-----------------");
                consumermetric.consume(consumer, Arrays.asList(topicName),fullStats,detailedStats,receiveSize, reportingInterval, startMs);
                endMs = System.currentTimeMillis();
                if(printMetrics){
                    metrics = consumer.metrics();               
                }
                consumer.close();
                elapsedSecs = (endMs - startMs) / 1000.0;
                fetchTimeInMs = Math.abs((endMs - startMs) - joinGroupTimeInMs.get());
                totalMBRead = (totalBytesRead.get() * 1.0) / (1024 * 1024);
                if(!detailedStats && fullStats){
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
                if(!detailedStats && !fullStats){
                    System.out.printf("%d records received, %.4f MB, %.4f MB/sec,  %.4f records/sec. %n",
                        totalMessagesRead.get(),
                        totalMBRead,
                        totalMBRead / elapsedSecs,
                        totalMessagesRead.get() / elapsedSecs
                    );
                }
                if(detailedStats){
                    consumermetric.printConsumerProgress(fullStats, bytesRead.get(), lastBytesRead, messagesRead, lastMessagesRead,
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
                .newFor("Client Performance Test")
                .build()
                .defaultHelp(true)
                .description("This tool is used to verify the client performance.");
        
        Subparsers subparsers = parser.addSubparsers()
                .dest("type")
                .help("Choose produce or consume")
                .description("Based on the subcommand, choose the parameter");

        Subparser produce = subparsers
                .addParser("produce")
                .defaultHelp(true)
                .description("Commands to produce");
        
        Subparser consume = subparsers
                .addParser("consume")
                .defaultHelp(true)
                .description("Commands to produce");
        
        MutuallyExclusiveGroup payloadOptions = produce
                .addMutuallyExclusiveGroup()
                .required(true)
                .description("Either --record-size or --payload-file must be specified but not both.");

        parser.addArgument("--server")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("BOOTSTRAP-SERVER")
                .dest("server")
                .help("Broker server to establish connection.");
        
        parser.addArgument("--topic")
                .action(store())
                .required(true)
                .type(String.class)
                .dest("topic-name")
                .metavar("TOPIC")
                .help("Produces or consumes to this topic.");

        produce.addArgument("--num-record")
                .action(store())
                .required(true)
                .type(Long.class)
                .metavar("NUM-RECORDS")
                .dest("numRecords")
                .help("Number of messages to produce.");

        produce.addArgument("--throughput")
                .action(store())
                .required(true)
                .type(Integer.class)
                .metavar("THROUGHPUT")
                .dest("throughput")
                .help("Set this to -1 to disable throttling.");

        consume.addArgument("--size")
                .action(store())
                .required(true)
                .type(Long.class)
                .metavar("RECEIVE-SIZE")
                .dest("receiveSize")
                .help("Number of messages to consume.");

        consume.addArgument("--interval")
                .action(store())
                .required(false)
                .setDefault(5000)
                .type(Integer.class)
                .metavar("REPORTING-INTERVAL-TIME")
                .dest("reportingInterval")
                .help("Interval in ms at which to print progress info.");
        
        consume.addArgument("--groupID")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("GROUP-ID")
                .dest("groupID")
                .help("Consumer Group id.");
         
        consume.addArgument("--detailed")
                .action(storeTrue())
                .type(Boolean.class)
                .required(false)
                .metavar("DETAILED-STATS")
                .dest("detailedStats")
                .help("Print detailed stats while consuming.");
 
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