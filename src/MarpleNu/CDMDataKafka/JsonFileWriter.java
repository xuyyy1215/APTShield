package MarpleNu.CDMDataKafka;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class JsonFileWriter implements DataWriter {

    private final JsonEncoder jsonEncoder;
    private final DatumWriter<Object> datumWriter;

    public JsonFileWriter(String schemaFile, String outputFilePath) throws Exception{

        File outputFile = new File(outputFilePath);
        if(outputFile == null || outputFile.getParentFile() == null || !outputFile.getParentFile().exists()){
            throw new Exception("Invalid file path: " + outputFilePath);
        }

        Parser parser = new Schema.Parser();
        Schema schema = parser.parse(new File(schemaFile));
        datumWriter = new SpecificDatumWriter<Object>(schema);
        OutputStream outputStream = new FileOutputStream(outputFile);
        jsonEncoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
    }

    @Override
    public void writeRecord(GenericContainer genericContainer) throws Exception {
        datumWriter.write(genericContainer, jsonEncoder);
    }

    @Override
    public void close() throws Exception {
        jsonEncoder.flush();
    }

}
