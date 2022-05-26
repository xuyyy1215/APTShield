package MarpleNu.Transform;

import MarpleNu.CDMDataReader.OfflineAvroFileReaderSingle;
import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;

import java.io.FileWriter;
import java.io.IOException;

public class Avro2Json {
    OfflineAvroFileReaderSingle offlineAvroFileReader;
    FileWriter jsonFileWriter;
    Avro2Json(String inputfile,String outputfile) throws IOException {
        offlineAvroFileReader = new OfflineAvroFileReaderSingle(inputfile);
        jsonFileWriter = new FileWriter(outputfile);
    }

    void start() throws IOException {
        TCCDMDatum tccdmDatum;
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
            Object o = tccdmDatum.getDatum();
            jsonFileWriter.write(tccdmDatum.toString()+'\n');
            /*
            switch (tccdmDatum.getType()){
                case RECORD_EVENT:
                    Event event = (Event) tccdmDatum.getDatum();
                    jsonFileWriter.write(event.toString()+'\n');
                    break;
                case RECORD_SUBJECT:
                    Subject subject = (Subject) tccdmDatum.getDatum();
                    jsonFileWriter.write(subject.toString()+'\n');
                    break;
                case RECORD_FILE_OBJECT:
                    FileObject fileObject = (FileObject) tccdmDatum.getDatum();
                    jsonFileWriter.write(fileObject.toString()+'\n');
                    break;
                case RECORD_IPC_OBJECT:
                    IpcObject ipcObject = (IpcObject)tccdmDatum.getDatum();
                    jsonFileWriter.write(ipcObject.toString()+'\n');
                case RECORD_NET_FLOW_OBJECT:

                case RECORD_SRC_SINK_OBJECT:
                case RECORD_PACKET_SOCKET_OBJECT:
                case RECORD_END_MARKER:
                    break;
                    case RECORD_HOST:
                case RECORD_PRINCIPAL:

            }
            */
        }
        jsonFileWriter.close();
    }

    public static void main(String[] args) throws IOException {
        Avro2Json avro2Json = new Avro2Json(args[0], args[1]);
        avro2Json.start();
    }
}
