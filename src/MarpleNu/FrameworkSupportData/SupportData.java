package MarpleNu.FrameworkSupportData;

import MarpleNu.CDMLinuxFrameworkMain.DetectionFramework;
import MarpleNu.FrameworkDataStruct.FrameworkFileInfo;
import MarpleNu.FrameworkDataStruct.FrameworkPrincipalInfo;
import MarpleNu.FrameworkDataStruct.FrameworkProcessInfo;
import MarpleNu.FrameworkReporter.E5reporter;
import MarpleNu.FrameworkReporter.Reporter;
import com.bbn.tc.schema.avro.cdm19.UUID;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
//主要初始化容器。
public class SupportData {
    public HashMap<UUID,FrameworkFileInfo> uuid2FileMap = new HashMap<>();
    public HashMap<Integer,FrameworkProcessInfo> tid2ProcessMap = new HashMap<>();
    public HashMap<Integer,FrameworkProcessInfo> ppid2ProcessMap = new HashMap<>();
    //public HashMap<UUID,FrameworkProcessInfo> uuid2ProcessMap = new HashMap<>();
    public HashMap<UUID,FrameworkPrincipalInfo> uuid2Principal = new HashMap<>();
    public static Date date = new Date();
    public DetectionFramework test = DetectionFramework.getInstance("Test");

    public static ArrayList<String> fileStored = new ArrayList<String>();


    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    public static Reporter reporter;

    static {
        try {
            reporter = new E5reporter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
