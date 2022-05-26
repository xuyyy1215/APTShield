package MarpleNu.CDMDataReader;

import MarpleNu.CDMDataKafka.JsonFileWriter;
import com.bbn.tc.schema.SchemaNotInitializedException;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericContainer;

import java.io.File;
import java.io.IOException;


public class OfflineAvroFileReaderSingle extends BaseDataReader{
    private AvroGenericDeserializer deserializer;
    private final boolean stopflag = true;
    public OfflineAvroFileReaderSingle(String fileName)
    {
        String schemaFile = "src/TCCDMDatum.avsc";
        File f = new File(fileName);
        try {
            deserializer = new AvroGenericDeserializer(schemaFile, schemaFile, true, f);
            System.out.println(deserializer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }
    }

    @Override
    public TCCDMDatum getData()
    {
        GenericContainer genericContainer = null;
        try {
            genericContainer = deserializer.deserializeNextRecordFromFile();
        } catch (SchemaNotInitializedException | IOException e) {
            e.printStackTrace();
        }
        if (genericContainer==null)
            return null;
        TCCDMDatum tccdmDatum = (TCCDMDatum)genericContainer;
        return tccdmDatum;
    }


    public static void main(String[] args) {


        String testFileName  = "avro/audit_cdm.avro";
        OfflineAvroFileReaderSingle offlineAvroFileReader = new OfflineAvroFileReaderSingle(testFileName);
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
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
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