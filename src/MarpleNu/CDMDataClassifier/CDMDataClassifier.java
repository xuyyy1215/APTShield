package MarpleNu.CDMDataClassifier;

import MarpleNu.CDMDataDump.AllDataDump;
import MarpleNu.CDMDataDump.DataDump;
import MarpleNu.CDMDataDump.E5DataDump;
import MarpleNu.CDMDataParser.*;
import MarpleNu.CDMDataReader.BaseDataReader;
import MarpleNu.CDMLinuxFrameworkMain.*;
import MarpleNu.FrameworkLable.FrameworkLable;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import com.bbn.tc.schema.avro.cdm19.*;
import scala.Int;
import scala.util.parsing.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CDMDataClassifier implements Runnable{
    private final BaseParser eventParser;
    private final BaseParser subjectParser;
    private final BaseParser objectParser;
    private final BaseParser principalParser;
    private final FrameworkLable frameworkLable;
    private final BaseDataReader baseDataReader;
    private Thread dataReader;
    private final SupportData supportData = new SupportData();
    private DataDump dataDump;
    private final boolean stopflag = true;
    private boolean dumpflag = true;
    private boolean detectFlag;
    private long sum = 0;
    private static final int DeadNumber = 10;
    private final long deadSum = 0;
    public static String topic = "";
    public static String hostid;

    public static int subject_num=0;

    public CDMDataClassifier(BaseDataReader baseDataReader) {
        this.baseDataReader = baseDataReader;
        //dataReader = new Thread(baseDataReader);
        //dataReader.start()；
        eventParser = new EventParser(supportData);
        subjectParser = new SubjectParser(supportData);
        objectParser = new ObjectParser(supportData);
        principalParser = new PrincipalParser(supportData);
        frameworkLable = new FrameworkLable(supportData);
    }

    @Override
    public void run()
    {
        dumpflag = FrameworkConfig.Search("dump_data").equals("true");
        detectFlag = FrameworkConfig.Search("detector_switch") == null || FrameworkConfig.Search("detector_switch").equals("true");
        String str = FrameworkConfig.Search("dump_way");
        switch (str){
            case "cmdline":
                try {
                    dataDump = new E5DataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "allInOne":
                try {
                    dataDump = new AllDataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                dataDump = new DataDump();
                break;
        }


        while (stopflag){
            TCCDMDatum tccdmDatum;
            try {



                tccdmDatum = baseDataReader.getData();


            } catch (Exception e){
                System.out.println(e.toString());
                continue;
            }
            if (tccdmDatum==null)
                continue;
            ++sum;
            if(sum==2)
            {
                //startTime1=((TimeMarker)tccdmDatum.datum).tsNanos;
                System.out.println(tccdmDatum.getDatum());
            }/*
            if(sum==1000001){
                sum=sum;
            }*/

            if (sum % 100000==0){
                System.out.printf("consumer %dk messages.%d\n",sum/1000,EventParser.MapEventLeft.size());
            }
            if (detectFlag) {

                Object datum = tccdmDatum.getDatum();
                //该日志属于事件日志
                if (datum instanceof Event) {
                    eventParser.parse(tccdmDatum);
                }
                //属于主体日志
                else if (datum instanceof Subject) {
                    subjectParser.parse(tccdmDatum);
                }
                //属于文件日志，注册表日志，网络流日志
                else if (datum instanceof FileObject ||
                        datum instanceof RegistryKeyObject ||
                        datum instanceof NetFlowObject ||
                        datum instanceof SrcSinkObject ||
                        datum instanceof IpcObject) {
                    objectParser.parse(tccdmDatum);
                } else if (datum instanceof Principal) {
                    principalParser.parse(tccdmDatum);
                } else if (datum instanceof Host) {
                    Host host = (Host) tccdmDatum.getDatum();
                    hostid = SupportData.byteArrayToHexString(host.getUuid().bytes());
                }

                if (datum instanceof EndMarker) {

                    System.out.println("EndMarker");

                }
            }
            if (dumpflag){

            }
            //处理日志信息，对主体客体加标签并判断是否发生了攻击。
            frameworkLable.label(tccdmDatum);
        }
    }
    public void writetoDisk(FileNode fileNode) throws IOException {
        File file = new File("./file.json");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
        fw.write(fileNode.getFilePath()+'\n');
        fw.close();

    }
}
