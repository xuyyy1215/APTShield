package MarpleNu.CDMDataKafka;

import org.apache.avro.generic.GenericContainer;

public interface DataWriter {

    void writeRecord(GenericContainer genericContainer) throws Exception;
    void close() throws Exception;

}