Client Performance Testing Tool
=================
This repository contains all the necessary files to test the client performance ( `consumer` and `producer` ) with Kafka brokers

## Contents
- [Prerequisite](#prerequisite)
- [Usage](#usage)
    - [Build the jar](#build-the-jar)
    - [Run the jar](#run-the-jar)
    - [Pass the appropriate arguments](#pass-the-appropriate-arguments)
    - [Arguments to be passed](#arguments-to-be-passed)
    - [Client Config Property File](#client-config-property-file)
    - [Arguments for sub commands](#arguments-for-sub-commands)
        - [Produce](#produce)
        - [Consume](#consume)

## Prerequisite
* **Java 8** — maven should be installed
* **kafka Broker** — minimum `three` should be deployed with maximum `CPU` and `Memory`
* **Zookeeper** — minimum `one` should be deployed with maximum `CPU` and `Memory`

## Usage
### Build the jar
`mvn install <folder-name>`

**Note:** Built with netbeans editor. If you are using any other IDE, check if main class is set.

### Run the jar
`java -jar <jar-name>`

**Note:** Graceful shutdown is handled while pressing CTRL + C.

### Pass the appropriate arguments  
    usage: Client Performance Test [-h] --server BOOTSTRAP-SERVER --topic TOPIC
                                   --throughput THROUGHPUT
                                   [--print-metrics]
                                   --client.config CLIENT-CONFIG-FILE
                                   {produce,consume} ...

    This tool is used to verify the client performance.

    named arguments:
      -h, --help             show this help message and exit
      --server BOOTSTRAP-SERVER
                             Broker server to establish connection.
      --topic TOPIC          Produces or consumes to this topic.
      --print-metrics        Print  out  metrics  at  the   end  of  the  test.
                             (default: false)
      --throughput THROUGHPUT
                             Set this to -1 to disable throttling.
      --client.config CLIENT-CONFIG-FILE
                             Client  config  properties  file  containing  both
                             ssl, producer and consumer properties.

    subcommands:
        Based on the subcommand, choose the parameter

        {produce,consume}      Choose produce or consume
  
### Arguments to be passed
**Global parameters for both producer and consumer**:
  * `--server localhost:9093`
  * `--topic newtopic `
  * `--client.config /etc/kafka/readiness.properties`
  * `--print-metrics ( default: false)`
  * `--throughput -1`
        
**Sub-Commands:**
  * `produce`
  * `consume`

### Client Config Property File
Pass the property file containing the below paramters for ssl based communication.

```
security.protocol=SSL
ssl.truststore.location=<truststore-jks-file-to-be-passed>
ssl.truststore.password=<password>
ssl.keystore.location=<keystore-jks-file-to-be-passed>
ssl.keystore.password=<password>
ssl.key.password=<password>
```
### Arguments for sub commands     
#### Produce
     `  usage: client-performance --server BOOTSTRAP-SERVER --topic TOPIC
                        --client.config CLIENT-CONFIG-FILE produce [-h] --num-record NUM-RECORDS
                        [--payload-delimiter PAYLOAD-DELIMITER]
                        (--size RECORD-SIZE | --payload-file PAYLOAD-FILE)
                
        Choose the below named arguments for producing        
                        
        named arguments:
          -h, --help              show this help message and exit
          --num-record NUM-RECORDS
                                  Number of messages to produce.
          --payload-delimiter PAYLOAD-DELIMITER
                                  Defaults to new line. This parameter  will be ignored if --payload-file is
                                  not provided. (default: \n)

          Either --record-size or --payload-file must be specified but not both.

          --size RECORD-SIZE      Message size in bytes.
          --payload-file PAYLOAD-FILE
                                  Payload for message to be  sent.  This  works  only for UTF-8 encoded text
                                  files. 
                                  
##### Sample Command
```java
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --server localhost:9093 --topic newtopic --client.config /etc/kafka/readiness.properties --throughput 10 produce 
--num-record 1000 --size 10
```
##### Sample Output
```
/var/tmp # java -jar lsv-0.1.0-SNAPSHOT-jar-with-dependencies.jar --server eric-bss-msg-kafka-client:9093 --topic four --client.config /var/tmp/readiness.properties 
--throughput -1 produce --num-record 100000 --size 5120

SSL enabled: true

Topic properties: {bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.key.password=clientPassword, ssl.truststore.password=clientPassword, customerpartitions=1, ssl.keystore.password=clientPassword, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, security.protocol=SSL}

Producer properties: {security.protocol=SSL, bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.keystore.password=clientPassword, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, customerpartitions=1, ssl.truststore.password=clientPassword, value.serializer=org.apache.kafka.common.serialization.ByteArraySerializer, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, retries=0, linger.ms=2, key.serializer=org.apache.kafka.common.serialization.StringSerializer, max.block.ms=5000, acks=1, ssl.key.password=clientPassword}

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
-----------------Starting Producer-----------------
13795 records sent, 67.36 MB, 2759.0 records/sec (13.47 MB/sec), 1434.9 ms avg latency, 1921.0 ms max latency, 13795 total records sent.
21837 records sent, 106.63 MB, 4367.4 records/sec (21.33 MB/sec), 1437.4 ms avg latency, 1721.0 ms max latency, 35632 total records sent.
22353 records sent, 109.15 MB, 4469.7 records/sec (21.82 MB/sec), 1378.7 ms avg latency, 1557.0 ms max latency, 57985 total records sent.
21447 records sent, 104.72 MB, 4289.4 records/sec (20.94 MB/sec), 1406.6 ms avg latency, 1514.0 ms max latency, 79432 total records sent.
20568 records sent, 100.43 MB, 830.5 records/sec (4.06 MB/sec), 1443.9 ms avg latency, 1558.0 ms max latency, 100000 total records sent.
Traffic is stopping now....

Started at: 01:18:37 PM 09-Mar-2022

Stopped at: 01:19:02 PM 09-Mar-2022

--------Producer Performance Summary Report--------

Count of total record sent: 100000 
Total MB sent:  488.28 MB
Records per second sent:  4037.79 records/sec
Mb per second:  19.72 MB/sec
Average latency in ms:  1418.69 ms 
Maximum latency in ms:  1921.00 ms 

--------------End Of Summary Report----------------
Shutting down producer.....
```
#### Consume
        usage: client-performance --server BOOTSTRAP-SERVER --topic TOPIC
                          --throughput THROUGHPUT
                          --client.config CLIENT-CONFIG-FILE consume [-h]
                          --size RECEIVE-SIZE
                          [--interval REPORTING-INTERVAL-TIME]
                          --groupID GROUP-ID [--no-logs] [--async] [--full]

        Choose the below named arguments for consuming

        named arguments:
          -h, --help             show this help message and exit
          --size RECEIVE-SIZE    Number of messages to consume.
          --interval REPORTING-INTERVAL-TIME
                                 Interval in ms at  which  to  print progress info. (default: 5000)
          --groupID GROUP-ID     Consumer Group id.
          --no-logs              Consume without printing logs. (default: false)
          --async                Commit offsets asynchronously. (default: false)
          --full                 Print complete  stats  while  consuming. (default: false)
          
##### Sample Command
```java
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --server localhost:9093 --topic newtopic --client.config /etc/kafka/readiness.properties --throughput -1 consume 
--size 1000 --groupID test
``` 
##### Sample Command
```
/var/tmp # java -jar lsv-0.1.0-SNAPSHOT-jar-with-dependencies.jar --server eric-bss-msg-kafka-client:9093 --topic four --client.config /var/tmp/readiness.properties 
--throughput -1 consume --groupID test --size 1000

SSL enabled: true

Topic properties: {bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.key.password=clientPassword, ssl.truststore.password=clientPassword, customerpartitions=1, ssl.keystore.password=clientPassword, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, security.protocol=SSL}

Consumer Properties: {key.deserializer=org.apache.kafka.common.serialization.StringDeserializer, value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.truststore.password=clientPassword, enable.auto.commit=false, security.protocol=SSL, group.id=test, check.crcs=false, auto.offset.reset=earliest, fetch.max.bytes=10, customerpartitions=1, ssl.keystore.password=clientPassword, bootstrap.servers=eric-bss-msg-kafka-client:9093, max.poll.records=1, ssl.key.password=clientPassword, max.partition.fetch.bytes=10, receive.buffer.bytes=10}

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
-----------------Starting Consumer-----------------
196 records received, 0.9570 MB, 0.1914 MB/sec,  0.0000 records/sec.
1000 records received, 4.8828 MB, 1.1275 MB/sec,  0.0000 records/sec.
Traffic is stopping now....

Started at: 01:21:37 PM 09-Mar-2022

Stopped at: 01:21:46 PM 09-Mar-2022

--------Consumer Performance Summary Report--------

Count of total record received: 1000 
Total MB received:  4.88 MB
Records per second received:  118.04 records/sec
Mb per second:  0.58 MB/sec
Offset committed: 999
Rebalance Time in ms:  4298 ms 
Fetch Time in ms:  4174 ms 
Fetch Mb per second:  1.17 MB/sec 
Fetch records per second:  239.58 records/sec 

--------------End Of Summary Report----------------
Shutting down consumer.....
```
