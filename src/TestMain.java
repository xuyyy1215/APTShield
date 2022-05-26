import MarpleNu.CDMDataClassifier.CDMDataClassifier;
import MarpleNu.CDMDataReader.*;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;

import java.io.IOException;

public class TestMain {


    public static void main(String[] args) throws IOException {
        FrameworkConfig.init();
        System.out.println(args[0]);

        BaseDataReader baseDataReader;
        switch (args[0]) {
            case "online":
                baseDataReader = new OnlineKafkaSSLReaderSingle();
                break;
            case "offline":
                baseDataReader = new OfflineAvroFileReaderSingle(args[1]);
                break;
            default:
                baseDataReader = new OnlineKafkaReader();
                break;
        }
        CDMDataClassifier cdmDataClassifier = new CDMDataClassifier(baseDataReader);
        Thread mainThread = new Thread(cdmDataClassifier);
        mainThread.start();
        System.out.println("jieshu");
    }
}
