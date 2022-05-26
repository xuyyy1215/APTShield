package MarpleNu.FrameworkReporter;

import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class E5reporter implements Reporter{
    KafkaProducer<String, String> kafkaProducer;
    private HashMap<String,String> kafkaConfigMap =  new HashMap<>();
    private final String kafkaServerAddresss;
    private final String kafkaGroupId;
    private final String kafkaTopic;

    public E5reporter() throws IOException {
        String ReporterPathKey = "reporter_conf_path";
        String path = FrameworkConfig.Search(ReporterPathKey);
        kafkaConfigMap = FrameworkConfig.buildmap(path);
        Properties properties = new Properties();
        kafkaServerAddresss = kafkaConfigMap.get("MarpleKakfaAddress");
        kafkaGroupId = kafkaConfigMap.get("MarpleKafkaGroupId");
        kafkaTopic = kafkaConfigMap.get("MarpleKafkaTopic");
        properties.put("bootstrap.servers", kafkaServerAddresss);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(properties);
    }

    @Override
    public void report(String content) {
        new Thread(() -> {
            kafkaProducer.send(new ProducerRecord<>(kafkaTopic, "", content));
            kafkaProducer.flush();
        }).start();
    }
}
