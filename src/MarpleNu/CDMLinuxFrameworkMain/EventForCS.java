package MarpleNu.CDMLinuxFrameworkMain;



import com.bbn.tc.schema.avro.cdm19.PrivilegeLevel;

public class EventForCS {
    // 0: ProcessStart
    // 1: ProcessEnd
    // 2: FileRead
    // 3: FileWrite
    // 4: Update Process Name
    // 5: Injection
    // 6: ImageLoad

    int eventType;
    public int uuid;
    public String subjectName="";
    public int pid;
    public long timestamp;
    int parentUuid = -1;
    public String processNameOrFilePath;
    String processNameOrFilePath2;
    public String pcid;
    PrivilegeLevel pLevel;


    public EventForCS(int eventType, int uuid, int pid, long timestamp, int parentUuid, String str1, String str2, String str3) {
        this(eventType, uuid, pid, timestamp, str1, str2);
        this.parentUuid = parentUuid;
        this.subjectName = str3;
    }

    public EventForCS(int eventType, int uuid, int pid, long timestamp, int parentUuid, String str1, String str2) {
        this(eventType, uuid, pid, timestamp, str1, str2);
        this.parentUuid = parentUuid;
    }
    public EventForCS(int eventType, int uuid, int pid, long timestamp, int parentUuid, String str1, String str2, PrivilegeLevel plevel){
        this(eventType, uuid, pid, timestamp, str1, str2);
        this.parentUuid = parentUuid;
        this.pLevel = plevel;
    }
    public EventForCS(int eventType, int uuid, int pid, long timestamp, String str1, String str2) {
        this.eventType = eventType;
        this.uuid = uuid;
        this.pid = pid;
        this.timestamp = timestamp;
        this.processNameOrFilePath = str1;
        this.processNameOrFilePath2 = str2;
        //local
        String saddr = "";
        //remote
        String daddr = "";
        int sport = -1;
        int dport = -1;
        pcid = "trace";
    }

    @Override
    public String toString() {
        return "EventForCS{" +
                "eventType=" + eventType +
                ", uuid=" + uuid +
                ", pid=" + pid +
                ", timestamp=" + timestamp +
                ", parentUuid=" + parentUuid +
                ", processNameOrFilePath='" + processNameOrFilePath + '\'' +
                ", processNameOrFilePath2='" + processNameOrFilePath2 + '\'' +
                '}';
    }
}
