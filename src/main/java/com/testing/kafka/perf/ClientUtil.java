package com.testing.kafka.perf;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
/**
 *
 * @author euimkks
 */
public class ClientUtil {
    private static boolean IS_SSL_ENABLED = true;
    private static Properties prop = new Properties();
    public static Properties props = new Properties();
    public static Properties commonProps = new Properties();
    public static Properties producerProps = new Properties();
    public static Properties consumerProps = new Properties();
    
    public ClientUtil(String clientConfig) throws Exception{
        props=readProperties(clientConfig);
    }
    public Properties getProducerProps(){
        getCommonProperties(producerProps);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
        producerProps.put(ProducerConfig.ACKS_CONFIG,"1");
        producerProps.put(ProducerConfig.RETRIES_CONFIG,0);
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG,2);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return producerProps;
    }

    public Properties getConsumerProps(){
        getCommonProperties(consumerProps);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProps.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, String.valueOf(10));
        consumerProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, String.valueOf(10));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.CHECK_CRCS_CONFIG, "false");
        consumerProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "10");
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        return consumerProps;
    }

    public void getCommonProperties(Properties commonProps){
        commonProps.put("customerpartitions", "1");
        if(IS_SSL_ENABLED) {
            commonProps.put("security.protocol",props.getProperty("security.protocol"));
            commonProps.put("ssl.truststore.location",props.getProperty("ssl.truststore.location"));
            commonProps.put("ssl.truststore.password",props.getProperty("ssl.truststore.password"));
            commonProps.put("ssl.keystore.location",props.getProperty("ssl.keystore.location"));
            commonProps.put("ssl.keystore.password",props.getProperty("ssl.keystore.password"));
            commonProps.put("ssl.key.password",props.getProperty("ssl.key.password"));
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
