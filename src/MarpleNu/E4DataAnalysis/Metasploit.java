package MarpleNu.E4DataAnalysis;

import MarpleNu.CDMDataKafka.JsonFileWriter;
import MarpleNu.CDMDataReader.OfflineAvroFileReaderSingle;
import MarpleNu.FrameworkTools.CDMDataTools;
import com.bbn.tc.schema.avro.cdm19.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Metasploit {
    private static final String[] fileNameList={
            "launchmyserver.sh"
    };

    static long sum=0;
    static byte[] bytes = {85, 99, -120, 38, -71, -91, -86, 21, -26, 73, 72, 9, 97, 42, -92, -99};
    static UUID process = new UUID(bytes);
    static Set<UUID> uuidSet = new HashSet<>();
    static Set<UUID> processSet = new HashSet<>();
    boolean isPartOfSSHAttack(TCCDMDatum tccdmDatum, FileWriter fileWritter) throws IOException {
        if (tccdmDatum.getType() == RecordType.RECORD_FILE_OBJECT){
            FileObject fileObject= (FileObject)tccdmDatum.getDatum();
            for (String filename:fileNameList) {
                if (CDMDataTools.getPropertiesValue(fileObject.getBaseObject(), "path").contains(filename)) {
                    //System.out.println(fileObject.toString());
                    fileWritter.write(fileObject.toString()+'\n');
                    uuidSet.add(fileObject.getUuid());
                }
            }
        }

        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (Event)tccdmDatum.getDatum();
            if ((event.getPredicateObject()!=null && event.getPredicateObject().equals(process)) ||
                    (event.getPredicateObject2()!=null && event.getPredicateObject2().equals(process))) {
                    //System.out.println(event.toString());
                    fileWritter.write(event.toString()+'\n');
                    processSet.add(event.getSubject());
                    return false;
                }
            for (UUID uuid:processSet){
                if (event.getSubject().equals(uuid)){
                    //System.out.println(event.toString());
                    fileWritter.write(event.toString()+'\n');
                    return false;
                }
            }
        }

        return false;
    }

    public static void main(String[] args) throws IOException {
        String testFileName  = "avro/output.avro";
        OfflineAvroFileReaderSingle offlineAvroFileReader = new OfflineAvroFileReaderSingle(testFileName);
        Thread thread = new Thread(offlineAvroFileReader);
        thread.start();
        JsonFileWriter jsonFileWriter = null;
        File file =new File("metasploit.json");
        if(!file.exists()){
            file.createNewFile();
        }
        FileWriter fileWritter = new FileWriter(file.getName(),false);

        try {
            jsonFileWriter = new JsonFileWriter("src/TCCDMDatum.avsc","src/test.out");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonFileWriter==null)
            return;
        TCCDMDatum tccdmDatum;
        Metasploit metasploit = new Metasploit();
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
            try {
                sum++;
                if (sum % 100000 ==0){
                    //System.out.printf("sum %dk\n",sum/1000);
                }
                if (metasploit.isPartOfSSHAttack(tccdmDatum,fileWritter))
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
        fileWritter.close();
    }
}
