Client Performance Testing Tool
=================
This repository contains all the necessary files to test the client performance both consumer and producer with Kafka brokers

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

### Pass the appropriate arguments  
    usage: Client Performance Test [-h] --server BOOTSTRAP-SERVER --topic TOPIC
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
                        --throughput THROUGHPUT [--payload-delimiter PAYLOAD-DELIMITER]
                        (--size RECORD-SIZE | --payload-file PAYLOAD-FILE)
        named arguments:
          -h, --help              show this help message and exit
          --num-record NUM-RECORDS
                                  Number of messages to produce.
          --throughput THROUGHPUT
                                  Set this to -1 to disable throttling.
          --payload-delimiter PAYLOAD-DELIMITER
                                  Defaults to new line. This parameter  will be ignored if --payload-file is
                                  not provided. (default: \n)

          Either --record-size or --payload-file must be specified but not both.

          --size RECORD-SIZE      Message size in bytes.
          --payload-file PAYLOAD-FILE
                                  Payload for message to be  sent.  This  works  only for UTF-8 encoded text
                                  files. 
                                  
##### Example Command
```java
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies --server localhost:9093 --topic newtopic --client.config /etc/kafka/readiness.properties produce --throughput 10 --num-record 1000 --size 10
```
         
#### Consume
     `  usage: client-performance --server BOOTSTRAP-SERVER --topic TOPIC
                          --client.config CLIENT-CONFIG-FILE consume [-h] --size RECEIVE-SIZE
                          [--interval REPORTING-INTERVAL-TIME] --groupID GROUP-ID [--detailed]
                          [--full]
        named arguments:
          -h, --help             show this help message and exit
          --size RECEIVE-SIZE    Number of messages to consume.
          --interval REPORTING-INTERVAL-TIME
                                 Interval in ms at which to print progress info. (default: 5000)
          --groupID GROUP-ID     Consumer Group id.
          --detailed             Print detailed stats while consuming. (default: false)
          --full                 Print complete stats while consuming. (default: false)
          
##### Example Command
```java
java -jar perf-0.1.0-SNAPSHOT-jar-with-dependencies.jar --server localhost:9093 --topic newtopic --client.config /etc/kafka/readiness.properties consume --size 1000 --groupID test
``` 
