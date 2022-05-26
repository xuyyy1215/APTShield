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

public class FirefoxAttack {
    private static final String[] fileNameList={
            //"launchmyserver.sh",
            //"usr/bin/sshd",
            //"tmp/libnet.so",
            "work/hosts"
    };
    private static final String[] processName={
            "firefox"
    };
    private static long sum=0;
    private static final byte[][] bytes ={
            /*
            {-29, 38, -65, 47, 53, -9, 121, 101, 48, 79, -83, -10, 35, -112, 90, 35},
            {-5, -12, 70, -113, 54, 9, 62, 53, 71, -56, 81, 98, 32, -43, 81, 20}
            */
            {121, 90, -107, 114, -122, 40, -100, -17, 42, -2, -118, 76, 118, 79, 18, 41},
            {9, -27, -121, 50, 20, -1, -40, 30, 46, 118, 44, 29, -53, 65, -36, 94},
            {-48, -26, 76, 124, 125, 22, -109, 42, 126, -123, 25, -27, 35, 40, 55, -77}
    };

    private static final byte[][] filebyte = {
            {41, -37, 6, 47, -100, -84, -43, 68, 84, -26, -106, -54, -100, -38, -68, -124},
            {8, -116, -40, 102, 71, 101, -60, 83, -70, 101, 9, -85, -112, 17, 62, -80},
            {-56, 20, 104, 3, 37, 83, 40, -46, -122, -12, -33, -30, -28, -9, -47, -43},
            {-28, -31, -57, 47, -14, -66, 91, -87, -84, 106, -87, 70, -52, -84, -106, 66},
            {-13, -33, -29, 27, 26, 76, -103, -101, 6, -10, 10, 77, -36, 52, -72, -11}
    };

    static UUID process;
    private static final HashSet<UUID> processList = new HashSet<UUID>();
    private static final HashSet<UUID> fileList = new HashSet<UUID>();
    private static final Set<UUID> uuidSet = new HashSet<>();
    static Set<UUID> processSet = new HashSet<>();
    private static void init(){
        for (byte[] bytes1:bytes){
            processList.add(new UUID(bytes1));
        }
        for (byte[] bytes1:filebyte){
            fileList.add(new UUID(bytes1));
        }
    }
    private boolean isPartOfFileList(TCCDMDatum tccdmDatum, FileWriter fileWritter) throws IOException {
        if (tccdmDatum.getType() == RecordType.RECORD_FILE_OBJECT){
            FileObject fileObject= (FileObject)tccdmDatum.getDatum();
            for (String filename:fileNameList) {
                if (CDMDataTools.getPropertiesValue(fileObject.getBaseObject(), "path").contains(filename)) {
                    System.out.println(fileObject.toString());
                    fileWritter.write(fileObject.toString() + '\n');
                    uuidSet.add(fileObject.getUuid());
                }
            }
        }

        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (Event)tccdmDatum.getDatum();
            for (UUID uuid:uuidSet) {

                if ((event.getPredicateObject()!=null && event.getPredicateObject().equals(uuid)) ||
                        (event.getPredicateObject2()!=null && event.getPredicateObject2().equals(uuid))) {
                   // System.out.println(event.toString());
                    fileWritter.write(event.toString() + '\n');
                }
            }
        }
        return false;
    }

    boolean isPartOfProcessList(TCCDMDatum tccdmDatum, FileWriter fileWritter) throws IOException {
        if (tccdmDatum.getType() == RecordType.RECORD_SUBJECT) {

            Subject subject = (Subject) tccdmDatum.getDatum();
            String cwd = "";
            for (CharSequence cs : subject.getProperties().keySet()) {
                switch (cs.toString()) {
                    case "cwd":
                        cwd = subject.getProperties().get(cs).toString();
                        break;
                }
            }

            if (processList.contains(subject.getParentSubject())) {
                processList.add(subject.getUuid());
            }

            if (processList.contains(subject.getUuid())) {
                System.out.println(subject.toString());
                fileWritter.write(subject.toString()+'\n');
            }
        }

        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (Event)tccdmDatum.getDatum();
            if (processList.contains(event.getSubject())) {
                fileWritter.write(event.toString() + '\n');
            }
        }



        return false;
    }

    boolean isPartOfFile(TCCDMDatum tccdmDatum, FileWriter fileWritter) {
        if (tccdmDatum.getType() == RecordType.RECORD_SRC_SINK_OBJECT){
            SrcSinkObject srcSinkObject = (SrcSinkObject)tccdmDatum.getDatum();
            if (fileList.contains(srcSinkObject.getUuid()))
                System.out.println(srcSinkObject.toString());
        }

        if (tccdmDatum.getType() == RecordType.RECORD_IPC_OBJECT){
            IpcObject ipcObject = (IpcObject)tccdmDatum.getDatum();
            if (fileList.contains(ipcObject.getUuid()))
                System.out.println(ipcObject.toString());
        }
        if (tccdmDatum.getType() == RecordType.RECORD_PACKET_SOCKET_OBJECT){
            PacketSocketObject packetSocketObject = (PacketSocketObject)tccdmDatum.getDatum();
            if (fileList.contains(packetSocketObject.getUuid()))
                System.out.println(packetSocketObject.toString());
        }
        if (tccdmDatum.getType() == RecordType.RECORD_NET_FLOW_OBJECT){
            NetFlowObject netFlowObject = (NetFlowObject)tccdmDatum.getDatum();
            if (fileList.contains(netFlowObject.getUuid()))
                System.out.println(netFlowObject.toString());
        }

        return false;
    }

    public static void main(String[] args) throws IOException {
        String testFileName  = "avro/ta1-trace-e4-B.avro";
        OfflineAvroFileReaderSingle offlineAvroFileReader = new OfflineAvroFileReaderSingle(testFileName);
        Thread thread = new Thread(offlineAvroFileReader);
        thread.start();
        JsonFileWriter jsonFileWriter = null;
        File file =new File("firefox.json");
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
        FirefoxAttack.init();
        FirefoxAttack firefoxAttack = new FirefoxAttack();
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
            try {
                sum++;
                if (sum % 100000 ==0){
                    System.out.printf("sum %dk\n",sum/1000);
                }
                firefoxAttack.isPartOfFileList(tccdmDatum,fileWritter);
                firefoxAttack.isPartOfProcessList(tccdmDatum,fileWritter);
                //firefoxAttack.isPartOfFile(tccdmDatum,fileWritter);
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    //e1.printStackTrace();
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
