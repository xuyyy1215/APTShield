package MarpleNu.CDMDataReader;


import MarpleNu.CDMDataKafka.JsonFileWriter;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;

import com.bbn.tc.schema.serialization.AvroConfig;
import com.bbn.tc.schema.serialization.AvroGenericSerializer;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class OnlineKafkaSSLReaderSingle extends BaseDataReader {
    private static final boolean flag = true;

    private final Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    private final KafkaConsumer<String, GenericContainer> consumer;
    private final long recordCounter = 0;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private int pollPeriod = 100;
    AvroGenericSerializer<GenericContainer> serializer;

    private final boolean setSpecificOffset = true;
    private final long forcedOffset = 0;
    private String schemaFilename="src/TCCDMDatum.avsc";
    private String groupId="123";
    private final Properties properties = new Properties();
    private HashMap<String,String> kafkaConfigMap =  new HashMap<>();


    private ConsumerRecords<String, GenericContainer> records = null;
    private Iterator<ConsumerRecord<String, GenericContainer>> recIter = null;

    private void confInit() throws IOException {
        String kafkaPathKey = "kafka_conf_path";
        String path = FrameworkConfig.Search(kafkaPathKey);
        kafkaConfigMap = FrameworkConfig.buildmap(path);
    }

    private void sslInit(){
        directInit();
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        String trustkeystorekey = "trustkeystore";
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaConfigMap.get(trustkeystorekey));
        String passwordkey = "password";
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaConfigMap.get(passwordkey));

        // configure the following three settings for SSL Authentication
        String keystoreKey = "keystore";
        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaConfigMap.get(keystoreKey));
        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaConfigMap.get(passwordkey));
        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaConfigMap.get(passwordkey));
    }

    private void directInit(){
        properties.put(ProducerConfig.ACKS_CONFIG, "all");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        //add some other properties
        String serverkey = "server";
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigMap.get(serverkey));
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 20000);
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 15000);
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "0"); //ensure no temporal batching
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // latest or earliestautoOffset

        //serialization properties
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                com.bbn.tc.schema.serialization.kafka.KafkaAvroGenericDeserializer.class);

        properties.put(AvroConfig.SCHEMA_READER_FILE, schemaFilename);
        properties.put(AvroConfig.SCHEMA_WRITER_FILE, schemaFilename);
        properties.put(AvroConfig.SCHEMA_SERDE_IS_SPECIFIC, true);
        properties.put("max.poll.records","100");

    }

    private void initKafkaReader() throws IOException {
        confInit();

        //configure the following three settings for SSL Encryption
        if (kafkaConfigMap.get("ssl").equals("true"))
            sslInit();
        else
            directInit();

    }


    public OnlineKafkaSSLReaderSingle() throws IOException {
        initKafkaReader();
        consumer = new KafkaConsumer<>(properties);
        TestConsumerRebalanceListener rebalanceListener = new TestConsumerRebalanceListener();
        String topickey = "topic";
        consumer.subscribe(Collections.singletonList(kafkaConfigMap.get(topickey)),rebalanceListener);
    }

    public OnlineKafkaSSLReaderSingle(String kafkaServer, String groupId, String topic, String schemaFilename, int pollPeriod) throws IOException {

        logger.setLevel(Level.DEBUG);
        this.pollPeriod = pollPeriod;
        this.groupId = groupId;
        this.schemaFilename = schemaFilename;
        initKafkaReader();

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic.split(",")));

    }

    public void shutdown()
    {
        closeConsumer();
    }


    ConsumerRecord<String, GenericContainer> record;
    @Override
    public TCCDMDatum getData()
    {
        if (records == null || !recIter.hasNext()) {
            records = consumer.poll(pollPeriod);
            recIter = records.iterator();
        }
        if (!recIter.hasNext())
            return null;
        return (TCCDMDatum) recIter.next().value();
    }


    private void closeConsumer() {
        if(consumer != null) {
            logger.info("Closing consumer session ...");
            consumer.commitSync();
            logger.info("Committed");
            consumer.unsubscribe();
            logger.info("Unsubscribed");
            consumer.close();
            logger.info("Consumer session closed.");
        }
    }

    public static void main(String[] args) throws IOException {
        long sum=0;
        FrameworkConfig.init();

        OnlineKafkaSSLReaderSingle onlineKafkaSSLReaderSingle = new OnlineKafkaSSLReaderSingle();
        JsonFileWriter jsonFileWriter = null;

        try {
            jsonFileWriter = new JsonFileWriter("src/TCCDMDatum.avsc","src/test.out");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonFileWriter==null)
            return;
        TCCDMDatum tccdmDatum;
        while (flag) {
            tccdmDatum = onlineKafkaSSLReaderSingle.getData();
            if (tccdmDatum==null)
                continue;
            sum++;
            if (sum%1000==0){
                System.out.printf("consum %dk\n",sum/1000);
            }

            try {
                jsonFileWriter.writeRecord(tccdmDatum);
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            //if (tccdmDatum.getType() == RECORD_END_MARKER)
                //break;
        }
        /*
        try {
            jsonFileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    private static class  TestConsumerRebalanceListener implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            System.out.println("Called onPartitionsRevoked with partitions:" + partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            System.out.println("Called onPartitionsAssigned with partitions:" + partitions);
        }
    }
}




