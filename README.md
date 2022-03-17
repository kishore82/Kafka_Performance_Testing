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
    usage: client-performance [-h] --throughput THROUGHPUT [--interval REPORTING-INTERVAL-TIME]
                          [--print-metrics] --client.config CLIENT-CONFIG-FILE {produce,consume}
                          ...

    This tool is used to verify the client performance.

    named arguments:
      -h, --help             show this help message and exit
      --interval REPORTING-INTERVAL-TIME
                             Interval in ms at which to print progress info. (default: 2000)
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
  * `--interval`
  * `--client.config /etc/kafka/readiness.properties`
  * `--print-metrics ( default: false)`
  * `--throughput -1`
        
**Sub-Commands:**
  * `produce`
  * `consume`

### Client Config Property File
Pass the property file containing the below paramters.

```
#Do not add extra properties. Changing values might impact performance

#ssl properties
security.protocol=SSL
ssl.truststore.location=/etc/kafkajks/client.truststore.jks
ssl.truststore.password=clientPassword
ssl.keystore.location=/etc/kafkajks/client.keystore.jks
ssl.keystore.password=clientPassword
ssl.key.password=clientPassword
bootstrap.servers=eric-bss-msg-kafka-client:9093

# general properties
# give comma separated values for topics
topicName=new1,new2,new3
partitions=5

# producer properties
# List of values: 0,1,-1
acks=1 
retries=2
linger.ms=1
max.block.ms=5000
# default value 16384 (16 kb)
batch.size=10240
# default value 131072 (128 kb)
send.buffer.bytes=51200
# default value 1048576 (1 mb)
max.request.size=51200
# List of values: none, gzip, snappy, lz4, or zstd
compression.type=none

#consumer properties
# default value 500
max.poll.records=500
group.id=second-r
# List of values: earliest, latest, none
auto.offset.reset=earliest
enable.auto.commit=false
allow.auto.create.topics=false
# default value 52428800 (50 mb)
fetch.max.bytes=10485760
check.crcs=false
# default value 1048576 (1 mb)
max.partition.fetch.bytes=65536
# default value 65536 (64 kb)
receive.buffer.bytes=65536
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
          --threadCount THREAD COUNT
                                  Number of threads.
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
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --client.config /var/tmp/readiness.properties --throughput 1000 --interval 5000 produce --threadCount 3 
--num-record 5000 --size 5120
```
##### Sample Output
```
/var/tmp # java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --client.config /var/tmp/readiness.properties --throughput 1000 --interval 5000 produce 
--threadCount 3 --num-record 5000 --size 5120

SSL enabled: true

Topic list: [topic1, topic2]

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
creating topic: topic1
creating topic: topic2
Topic properties: {bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.key.password=clientPassword, ssl.truststore.password=clientPassword, ssl.keystore.password=clientPassword, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, security.protocol=SSL}

Producer properties: {security.protocol=SSL, ssl.keystore.password=clientPassword, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, bootstrap.servers=eric-bss-msg-kafka-client:9093, max.request.size=51200, ssl.truststore.password=clientPassword, value.serializer=org.apache.kafka.common.serialization.ByteArraySerializer, send.buffer.bytes=51200, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, retries=2, linger.ms=1, key.serializer=org.apache.kafka.common.serialization.StringSerializer, batch.size=10240, max.block.ms=5000, compression.type=none, acks=1 , ssl.key.password=clientPassword}

-----------------Starting Producer-----------------
4999 records sent, 24.41 MB, 999.4 records/sec (4.88 MB/sec), 29.4 ms avg latency, 337.0 ms max latency, 4999 total records sent.
-----------------ProducerThread-0 Completed-----------------
5007 records sent, 24.45 MB, 1001.2 records/sec (4.89 MB/sec), 3.2 ms avg latency, 116.0 ms max latency, 10006 total records sent.
5002 records sent, 24.42 MB, 1000.2 records/sec (4.88 MB/sec), 2.0 ms avg latency, 18.0 ms max latency, 15008 total records sent.
-----------------ProducerThread-1 Completed-----------------
5009 records sent, 24.46 MB, 1001.4 records/sec (4.89 MB/sec), 2.7 ms avg latency, 98.0 ms max latency, 20017 total records sent.
4997 records sent, 24.40 MB, 999.2 records/sec (4.88 MB/sec), 4.4 ms avg latency, 110.0 ms max latency, 25014 total records sent.
-----------------ProducerThread-2 Completed-----------------
4986 records sent, 24.35 MB, 166.1 records/sec (0.81 MB/sec), 2.7 ms avg latency, 61.0 ms max latency, 30000 total records sent.
Traffic is stopping now....

Started at: 03:26:23 PM 17-Mar-2022

Stopped at: 03:26:23 PM 17-Mar-2022

--------Producer Performance Summary Report--------

Count of total record sent: 30000 
Total record sent per thread: 10000 
Total record sent per thread per topic: 5000 
Total MB sent:  146.48 MB
Records per second sent:  999.67 records/sec
Mb per second:  4.88 MB/sec
Average latency in ms:  7.40 ms 
Maximum latency in ms:  337.00 ms 

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
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --client.config /var/tmp/readiness.properties --throughput 1000 --interval 5000 consume --size 30000
``` 
##### Sample Command
```
/var/tmp # java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --client.config /var/tmp/readiness.properties --throughput 1000 --interval 5000 consume 
--size 30000

SSL enabled: true

Topic list: [topic1, topic2]

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Topic already exits: topic1
Topic already exits: topic2
Topic properties: {bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.key.password=clientPassword, ssl.truststore.password=clientPassword, ssl.keystore.password=clientPassword, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, security.protocol=SSL}

Consumer Properties: {key.deserializer=org.apache.kafka.common.serialization.StringDeserializer, value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer, ssl.keystore.location=/etc/kafkajks/client.keystore.jks, ssl.truststore.location=/etc/kafkajks/client.truststore.jks, ssl.truststore.password=clientPassword, enable.auto.commit=false, security.protocol=SSL, group.id=second-r, check.crcs=false, auto.offset.reset=earliest, fetch.max.bytes=10485760, bootstrap.servers=eric-bss-msg-kafka-client:9093, ssl.keystore.password=clientPassword, max.poll.records=500, ssl.key.password=clientPassword, allow.auto.create.topics=false, max.partition.fetch.bytes=65536, receive.buffer.bytes=65536}

-----------------Starting Consumer-----------------
2317 records received, 11.3135 MB, 2.2586 MB/sec,  0.0000 records/sec.
10013 records received, 48.8916 MB, 7.4812 MB/sec,  1000.0000 records/sec.
15053 records received, 73.5010 MB, 4.8877 MB/sec,  1000.0000 records/sec.
20061 records received, 97.9541 MB, 4.8838 MB/sec,  1000.0000 records/sec.
25069 records received, 122.4072 MB, 4.8838 MB/sec,  1000.0000 records/sec.
-----------------ConsumerThread Completed-----------------
30000 records received, 146.4844 MB, 4.4341 MB/sec,  0.0000 records/sec.
Traffic is stopping now....

Started at: 03:28:01 PM 17-Mar-2022

Stopped at: 03:28:32 PM 17-Mar-2022

--------Consumer Performance Summary Report--------

Count of total record received: 30000 
Total MB received:  146.48 MB
Records per second received:  983.93 records/sec
Mb per second:  4.80 MB/sec
Rebalance Time in ms:  3189 ms 
Fetch Time in ms:  27301 ms 
Fetch Mb per second:  5.37 MB/sec 
Fetch records per second:  1098.86 records/sec 

--------------End Of Summary Report----------------


-------------Consumer Offset Management------------

Beginning offsets: {"topic1-1":0,"topic2-1":0,"topic1-0":0,"topic2-0":0}
Current offsets: {"topic1-1":7500,"topic1-0":7500,"topic2-1":7500,"topic2-0":7500}
Ending offsets: {"topic1-1":7500,"topic2-1":7500,"topic1-0":7500,"topic2-0":7500}

----------------End of Offset Report---------------

Shutting down consumer.....
```
