package MarpleNu.E4DataAnalysis;


import com.bbn.tc.schema.avro.cdm19.*;
import MarpleNu.CDMDataKafka.JsonFileWriter;
import MarpleNu.CDMDataReader.OfflineAvroFileReaderSingle;
import java.util.HashSet;
import java.util.Set;

public class sshattack {
    private static final String[] fileNameList={
        "ccleaner",
        "taken2_linux_2.exe",
            "lock.png"
    };

    static long sum=0;
    static byte[] bytes = {50, -64, 41, -58, -45, 17, -41, -71, 121, 98, 126, 29, 45, -128, -62, 30};
    static UUID process = new UUID(bytes);
    static Set<UUID> uuidSet = new HashSet<>();
    boolean isPartOfSSHAttack(TCCDMDatum tccdmDatum){
        /*
        if (tccdmDatum.getType() == RecordType.RECORD_SUBJECT){
            Subject subject = (com.bbn.tc.schema.avro.cdm19.Subject)tccdmDatum.getDatum();
            if (subject.getUuid().equals(process))
                System.out.println(subject.toString());
        }
        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (com.bbn.tc.schema.avro.cdm19.Event)tccdmDatum.getDatum();
            if (event.getSubject().equals(process))
                System.out.println(event.toString());
        }
*/
        if (tccdmDatum.getType() == RecordType.RECORD_SRC_SINK_OBJECT){
            SrcSinkObject srcSinkObject = (SrcSinkObject)tccdmDatum.getDatum();
            if (srcSinkObject.getUuid().equals(process))
                System.out.println(srcSinkObject.toString());
        }

        if (tccdmDatum.getType() == RecordType.RECORD_IPC_OBJECT){
            IpcObject ipcObject = (IpcObject)tccdmDatum.getDatum();
            if (ipcObject.getUuid().equals(process))
                System.out.println(ipcObject.toString());
        }
        if (tccdmDatum.getType() == RecordType.RECORD_PACKET_SOCKET_OBJECT){
            PacketSocketObject packetSocketObject = (PacketSocketObject)tccdmDatum.getDatum();
            if (packetSocketObject.getUuid().equals(process))
                System.out.println(packetSocketObject.toString());
        }
        if (tccdmDatum.getType() == RecordType.RECORD_NET_FLOW_OBJECT){
            NetFlowObject netFlowObject = (NetFlowObject)tccdmDatum.getDatum();
            if (netFlowObject.getUuid().equals(process))
                System.out.println(netFlowObject.toString());
        }

        if (tccdmDatum.getType() == RecordType.RECORD_FILE_OBJECT){
            FileObject fileObject = (FileObject)tccdmDatum.getDatum();
            if (fileObject.getUuid().equals(process))
                System.out.println(fileObject.toString());
        }

        /*
        if (tccdmDatum.getType() == RecordType.RECORD_FILE_OBJECT){
            FileObject fileObject= (com.bbn.tc.schema.avro.cdm19.FileObject)tccdmDatum.getDatum();
            for (String filename:fileNameList) {
                if (CDMDataTools.getPropertiesValue(fileObject.getBaseObject(), "path").contains(filename)) {
                    System.out.println(fileObject.toString());
                    uuidSet.add(fileObject.getUuid());
                }
            }
        }

        if (tccdmDatum.getType() == RecordType.RECORD_EVENT){
            Event event = (com.bbn.tc.schema.avro.cdm19.Event)tccdmDatum.getDatum();
            for (UUID uuid:uuidSet) {

                if ((event.getPredicateObject()!=null && event.getPredicateObject().equals(uuid)) ||
                        (event.getPredicateObject2()!=null && event.getPredicateObject2().equals(uuid))) {
                    System.out.println(event.toString());
                }
            }
        }
        */
        return false;
    }

    public static void main(String[] args) {
        String testFileName  = "avro/output.avro";
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
        sshattack sshattack = new sshattack();
        while ((tccdmDatum = offlineAvroFileReader.getData()) != null) {
            try {
                sum++;
                if (sum % 100000 ==0){
                    //System.out.printf("sum %dk\n",sum/1000);
                }
                if (sshattack.isPartOfSSHAttack(tccdmDatum))
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
