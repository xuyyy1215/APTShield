package MarpleNu.E4DataAnalysis;

import MarpleNu.CDMDataKafka.JsonFileWriter;
import MarpleNu.CDMDataReader.OfflineAvroFileReaderSingle;
import com.bbn.tc.schema.avro.cdm19.*;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class azazel {
    private static final String[] fileNameList={
            "work/hosts"
    };

    private static long sum=0;
    private static final byte[][] bytes =
            {{-76, 49, 21, -56, -89, 113, 111, -41, -13, 49, -7, -110, -93, 40, 26, 86}
    };

    private static UUID process;
    private static final HashSet<UUID> processList = new HashSet<UUID>();
    static Set<UUID> uuidSet = new HashSet<>();
    static Set<UUID> processSet = new HashSet<>();
    private static void init(){
        for (byte[] bytes1:bytes){
            processList.add(new UUID(bytes1));
        }
    }
    private boolean isPartOfProcessList(TCCDMDatum tccdmDatum, FileWriter fileWritter) throws IOException {
        if (tccdmDatum.getType() == RecordType.RECORD_SUBJECT) {
            Subject subject = (Subject) tccdmDatum.getDatum();
            if (processList.contains(subject.getParentSubject())) {
                processList.add(subject.getUuid());
            }
            if (subject.getCid() == 19289){
                processList.add(subject.getUuid());
            }
            if (processList.contains(subject.getUuid())) {
                System.out.println(subject.toString());
                fileWritter.write(subject.toString()+'\n');
            }
        }
        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (Event)tccdmDatum.getDatum();
            if (processList.contains(event.getSubject()))
                fileWritter.write(event.toString()+'\n');
            switch (event.getType()){
                case EVENT_LOADLIBRARY:
                    if (event.getPredicateObjectPath().toString().contains("launchmyserver"))
                    System.out.println(event.toString());
                    break;
                    default:
                        break;
            }
        }
        return false;
    }

    boolean isPartOfSSHAttack(TCCDMDatum tccdmDatum, FileWriter fileWritter) throws IOException {
        if (tccdmDatum.getType() == RecordType.RECORD_SUBJECT) {
            Subject subject = (Subject) tccdmDatum.getDatum();
            if (subject.getUuid().equals(process)) {
                System.out.println(subject.toString());
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
        azazel.init();
        azazel aza = new azazel();
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
            try {
                sum++;
                if (sum % 100000 ==0){
                    System.out.printf("sum %dk\n",sum/1000);
                }
                if (aza.isPartOfProcessList(tccdmDatum,fileWritter))
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
