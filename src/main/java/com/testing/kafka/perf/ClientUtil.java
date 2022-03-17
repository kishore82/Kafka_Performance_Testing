package com.testing.kafka.perf;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
/**
 *
 * @author euimkks
 */
public final class ClientUtil {
    private static boolean IS_SSL_ENABLED = true;
    private static Properties prop = new Properties();
    public static Properties props = new Properties();
    public static Properties commonProps = new Properties();
    public static Properties producerProps = new Properties();
    public static Properties consumerProps = new Properties();
    public static List<String> topicList;

    public ClientUtil(String clientConfig) throws Exception{
        props=readProperties(clientConfig);
        topicList = Arrays.asList(props.getProperty("topicName").trim().replaceAll("\\s*,\\s*", ",").split(","));
        System.out.println("Topic list: "+topicList.toString()+"\n");
        for(String topicName: topicList){
            createTopic(topicName,Integer.valueOf(props.getProperty("partitions")),props);
        }
    }
    public Properties getProducerProps(){
        getCommonProperties(producerProps);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
        producerProps.put(ProducerConfig.ACKS_CONFIG,props.getProperty("acks"));
        producerProps.put(ProducerConfig.RETRIES_CONFIG,props.getProperty("retries"));
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG,props.getProperty("batch.size"));
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG,props.getProperty("linger.ms"));
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, props.getProperty("max.block.ms"));
        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, props.getProperty("compression.type"));
        producerProps.put(ProducerConfig.SEND_BUFFER_CONFIG, props.getProperty("send.buffer.bytes"));
        producerProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, props.getProperty("max.request.size"));
        return producerProps;
    }

    public Properties getConsumerProps(){
        getCommonProperties(consumerProps);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProps.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, props.getProperty("receive.buffer.bytes"));
        consumerProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, props.getProperty("max.partition.fetch.bytes"));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, props.getProperty("auto.offset.reset"));
        consumerProps.put(ConsumerConfig.CHECK_CRCS_CONFIG, props.getProperty("check.crcs"));
        consumerProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, props.getProperty("fetch.max.bytes"));
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, props.getProperty("max.poll.records"));
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, props.getProperty("enable.auto.commit"));
        consumerProps.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,props.getProperty("allow.auto.create.topics"));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,props.getProperty("group.id"));
        return consumerProps;
    }

    public void getCommonProperties(Properties commonProps){
        commonProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        if(IS_SSL_ENABLED) {
            commonProps.put("security.protocol",props.getProperty("security.protocol"));
            commonProps.put("ssl.truststore.location",props.getProperty("ssl.truststore.location"));
            commonProps.put("ssl.truststore.password",props.getProperty("ssl.truststore.password"));
            commonProps.put("ssl.keystore.location",props.getProperty("ssl.keystore.location"));
            commonProps.put("ssl.keystore.password",props.getProperty("ssl.keystore.password"));
            commonProps.put("ssl.key.password",props.getProperty("ssl.key.password"));
        }
    }

    public void createTopic(String topicName, int numPartitions, Properties adminProps) throws Exception {
        getCommonProperties(adminProps);
        try (AdminClient admin = AdminClient.create(adminProps)) {
            boolean alreadyExists = admin.listTopics().names().get().stream().anyMatch(existingTopic -> existingTopic.equals(topicName));
            if (alreadyExists) {
                System.out.printf("Topic already exits: %s", topicName+"\n");
            } else {
                System.out.printf("creating topic: %s", topicName+"\n");
                NewTopic newTopic = new NewTopic(topicName, numPartitions, (short) 1);
                admin.createTopics(Collections.singleton(newTopic)).all().get();
            }
        }
    }    

    private Properties readProperties(String filename) throws Exception
    {
        try(InputStream input = new FileInputStream(filename)) {
            prop.load(input);
            IS_SSL_ENABLED = prop.getProperty("security.protocol").equalsIgnoreCase("SSL");
            System.out.println("SSL enabled: "+IS_SSL_ENABLED+"\n");
            return prop;
        }
        catch (Exception ex)
        {
            System.out.println("Error loading file: {} "+ex+"\n");
        }
        return null;
    }
    
    public List<byte[]> readPayloadFromFile(String payloadFile, String payloadDelimiter) throws IOException{
        List<byte[]> payloadList = new ArrayList<>();
        if (payloadFile != null) {
            Path path = Paths.get(payloadFile);
            System.out.println("Reading payload from path: " + path.toAbsolutePath()+"\n");
            if (Files.notExists(path) || Files.size(path) == 0)  {
                throw new  IllegalArgumentException("File does not exist or empty file provided.");
            }
            String[] payloadArray = new String(Files.readAllBytes(path), "UTF-8").split(payloadDelimiter);
            for (String i : payloadArray) {
                payloadList.add(i.getBytes(StandardCharsets.UTF_8));
            }
        }
        return payloadList;
    }
    
    public byte[] generateRandomByte(Integer recordSize){
        byte[] payload = null;
        Random random = new Random(0);
        if (recordSize != null) {
            payload = new byte[recordSize];
            for (int i = 0; i < payload.length; ++i)
                payload[i] = (byte) (random.nextInt(26) + 65);
        }
        return payload;
    }
}
