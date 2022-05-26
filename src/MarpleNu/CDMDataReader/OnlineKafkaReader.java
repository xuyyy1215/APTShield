package MarpleNu.CDMDataReader;


import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroConfig;
import com.bbn.tc.schema.serialization.AvroGenericSerializer;
import org.apache.avro.generic.GenericContainer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnlineKafkaReader extends BaseDataReader {
    private final Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    private final KafkaConsumer<String, GenericContainer> consumer;
    private long recordCounter = 0;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private int pollPeriod = 100;
    AvroGenericSerializer<GenericContainer> serializer;

    private final boolean setSpecificOffset = true;
    private final long forcedOffset = 0;
    private String topic;
    private String schemaFilename="src/TCCDMDatum.avsc";
    private String kafkaServer="127.0.0.1:9096";
    private String groupId="MARPLE";
    private final Properties properties = new Properties();

    private void initKafkaReader()
    {
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        //add some other properties
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 20000);
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

//        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
//        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/var/private/ssl/kafka.client.truststore.jks");
//        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "TransparentComputing");
//        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/var/private/ssl/kafka.client.keystore.jks");
//        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "TransparentComputing");
//        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "TransparentComputing");

    }


    public OnlineKafkaReader()
    {
        initKafkaReader();
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic.split(",")));
    }

    public OnlineKafkaReader(String kafkaServer, String groupId, String topic, String schemaFilename, int pollPeriod) {

        logger.setLevel(Level.DEBUG);
        this.pollPeriod = pollPeriod;
        this.topic = topic;
        this.groupId = groupId;
        this.schemaFilename = schemaFilename;
        this.kafkaServer = kafkaServer;
        initKafkaReader();

        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic.split(",")));

    }

    public void setShutdown(){
        this.shutdown.set(true);
    }

    @Override
    public void run(){
        logger.info("Started KafkaReader");
        recordCounter = 0;
        PrintWriter output =null;
        boolean receivedSomethingYet = false;
        ConsumerRecords<String, GenericContainer> records;
        /*
        File testFile = new File("testRecordsAvro1.json");
        try {

            serializer = new AvroGenericSerializer("src/TCCDMDatum.avsc", true, testFile, true);

        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        */
        try{
            while (!shutdown.get()) {
                // =================== <KAFKA consumer> ===================
                records = consumer.poll(pollPeriod);
                Iterator<ConsumerRecord<String, GenericContainer>> recIter = records.iterator();
                ConsumerRecord<String, GenericContainer> record;
                while (recIter.hasNext()){
                    record = recIter.next();
                    //serializer.serializeToFile(record.value());
                    TCCDMDatum CDMdatum = (TCCDMDatum) record.value();
                }
                // =================== </KAFKA consumer> ===================
            }
            closeConsumer();
            logger.info("Done.");
        } catch (Exception e){
            logger.error("Error while consuming from Kafka", e);
//            e.printStackTrace();
        }
    }

    public String getTopic() {
        return topic;
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
}
