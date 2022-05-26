package MarpleNu.CDMDataReader;

import MarpleNu.CDMDataKafka.JsonFileWriter;
import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericContainer;

import java.io.File;
import java.io.IOException;

public class OfflineAvroFileReader extends BaseDataReader{
    private AvroGenericDeserializer deserializer;
    private final boolean stopflag = true;
    private OfflineAvroFileReader(String fileName)
    {
        String schemaFile = "src/TCCDMDatum.avsc";
        File f = new File(fileName);
        try {
            deserializer = new AvroGenericDeserializer(schemaFile, schemaFile, true, f);
        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }
    }

    @Override
    public void run()
    {
        GenericContainer genericContainer;
        while (stopflag) {
            try {
                genericContainer = deserializer.deserializeNextRecordFromFile();
            } catch (SchemaNotInitializedException | IOException e) {
                e.printStackTrace();
                continue;
            }
            if (genericContainer==null)
                return;
            tccdmDatumList.addLast((TCCDMDatum)genericContainer);
        }
    }

    public static void main(String[] args) {
        String testFileName  = "avro/audit_cdm.avro";
        OfflineAvroFileReader offlineAvroFileReader = new OfflineAvroFileReader(testFileName);
        Thread thread = new Thread(offlineAvroFileReader);
        thread.start();
        JsonFileWriter jsonFileWriter = null;
        try {
            jsonFileWriter = new JsonFileWriter("src/TCCDMDatum.avsc","src/test.out");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonFileWriter==null)
            return;
        TCCDMDatum tccdmDatum;
        while (thread.isAlive() || offlineAvroFileReader.getSize()>0) {
            tccdmDatum = offlineAvroFileReader.getData();
            try {
                jsonFileWriter.writeRecord(tccdmDatum);
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        try {
            jsonFileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
