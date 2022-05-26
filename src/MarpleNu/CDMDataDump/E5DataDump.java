package MarpleNu.CDMDataDump;

import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import com.bbn.tc.schema.avro.cdm19.*;
import javafx.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class E5DataDump extends DataDump{
    private final File rootDir;
    private final File basic;
    private final HashMap<String,String> DumpConfigMap;
    private final HashMap<UUID,HostInfo> HostMap = new HashMap<>();
    private final HashMap<Integer,UUID> subjectMap = new HashMap<>();
    private final HashMap<Integer,CharSequence> subjectNameMap = new HashMap<>();
    private final FileWriter baiscFileWriter;

    public  E5DataDump() throws IOException {
        String path = FrameworkConfig.Search("dump_conf_path");
        DumpConfigMap = FrameworkConfig.buildmap(path);
        path = DumpConfigMap.get("output_dir");
        rootDir = new File(path);
        if (!rootDir.exists())
            rootDir.mkdirs();
        basic = new File(path+"/basic.json");
        baiscFileWriter = new FileWriter(rootDir.getPath()+"/basic.json");

    }

    @Override
    public void dump(TCCDMDatum tccdmDatum) throws IOException {
        switch (tccdmDatum.getType()){
            case RECORD_HOST:
                Host host = (Host)tccdmDatum.getDatum();
                HostMap.put(tccdmDatum.getHostId(),new HostInfo(rootDir,host.getHostName().toString()));
                baiscFileWriter.write(tccdmDatum.toString()+'\n');
                break;
            case RECORD_END_MARKER:
                baiscFileWriter.write(tccdmDatum.toString()+'\n');
                break;
            case RECORD_PRINCIPAL:
                Principal principal = (Principal)tccdmDatum.getDatum();
                HostMap.get(tccdmDatum.getHostId()).putPrincipal(principal.getUuid(),
                        principal.getUserId()==null?"unknow":principal.getUserId().toString());
                HostMap.get(tccdmDatum.getHostId()).dumpData(null,WriteType.principal,tccdmDatum);
                break;
            case RECORD_NET_FLOW_OBJECT:
                NetFlowObject netFlowObject = (NetFlowObject)tccdmDatum.getDatum();
                HostMap.get(tccdmDatum.getHostId()).dumpData(null,WriteType.net,tccdmDatum);
                break;
            case RECORD_FILE_OBJECT:
                FileObject fileObject = (FileObject)tccdmDatum.getDatum();
                HostMap.get(tccdmDatum.getHostId()).dumpData(fileObject.getLocalPrincipal(),WriteType.file,tccdmDatum);
                break;
            case RECORD_IPC_OBJECT:
                IpcObject ipcObject = (IpcObject)tccdmDatum.getDatum();
                HostMap.get(tccdmDatum.getHostId()).dumpData(null,WriteType.ipc,tccdmDatum);
                break;
            case RECORD_SUBJECT:
                Subject subject = (Subject)tccdmDatum.getDatum();
                subjectMap.put(subject.getCid(),subject.getLocalPrincipal());
                subjectNameMap.put(subject.getCid(),subject.getCmdLine()==null?"unknow":subject.getCmdLine());
                HostMap.get(tccdmDatum.getHostId()).dumpData(subject.getLocalPrincipal(),WriteType.subject,tccdmDatum);
                break;
            case RECORD_EVENT:
                Event event = (Event)tccdmDatum.getDatum();
                HostMap.get(tccdmDatum.getHostId()).dumpData(subjectMap.get(event.getThreadId()),
                        subjectNameMap.get(event.getThreadId()),tccdmDatum);
                break;
        }
    }
}

class HostInfo{
    private final String name;
    private final File hostDumpDir;
    private final HashMap<UUID, PrincipalInfo> principalInfoHashMap;
    private final FileWriter fileWriterNet;
    private final FileWriter fileWriterIpc;
    private final FileWriter fileWriterFile;
    private final FileWriter fileWriterPrincipal;
    private final FileWriter fileWriterAlert;
    HostInfo(File rootdir, String name) throws IOException {
        this.name = name;
        hostDumpDir = new File(rootdir,name);
        hostDumpDir.mkdirs();
        fileWriterNet = new FileWriter(hostDumpDir.getPath()+"/net",false);
        fileWriterIpc = new FileWriter(hostDumpDir.getPath()+"/ipc",false);
        fileWriterFile = new FileWriter(hostDumpDir.getPath()+"/file",false);
        fileWriterPrincipal = new FileWriter(hostDumpDir.getPath()+"/principal",false);
        fileWriterAlert = new FileWriter(hostDumpDir.getPath()+"/alert",false);
        principalInfoHashMap = new HashMap();
    }
    void putPrincipal(UUID uuid, String name) throws IOException {
        principalInfoHashMap.put(uuid,new PrincipalInfo(fileWriterAlert,hostDumpDir,name));
    }

    void dumpData(UUID uuid, WriteType writeType,TCCDMDatum tccdmDatum) throws IOException {
        switch (writeType) {
            case file:
                fileWriterFile.write(tccdmDatum.toString()+'\n');
                break;
            case subject:
                principalInfoHashMap.get(uuid).dumpData(writeType, tccdmDatum);
                break;
            case net:
                fileWriterNet.write(tccdmDatum.toString()+'\n');
                break;
            case ipc:
                fileWriterIpc.write(tccdmDatum.toString()+'\n');
                break;
            case principal:
                fileWriterPrincipal.write(tccdmDatum.toString()+'\n');
                break;
        }
    }
    void dumpData(UUID uuid, CharSequence subject,TCCDMDatum event) throws IOException {
        if (principalInfoHashMap.containsKey(uuid)){
            principalInfoHashMap.get(uuid).dumpData(subject.toString(),event);
        }

    }
}

class PrincipalInfo{
    private final File principalDumpDir;
    private String hostDir;
    private final HashMap<WriteType,FileWriter> writeTypeFileWriterHashMap = new HashMap<>();
    //private HashMap<String,FileWriter> subjectEvent = new HashMap<>();
    private final HashMap<String,Integer> subject2Number = new HashMap<>();
    //private HashMap<EventType, Event> reduceData = new HashMap<>();
    private final FileWriter fileWriterNode;
    private final FileWriter fileWriterAlert;
    private static Integer startNode = 0;
    String[] E4CMDLine = {
            "fuser -k",
            "netstst",
            "cat /etc/hosts",
            "cat /etc/passwd",
            "cat /etc/shadow",
            "ifconfig",
            "wall -P",
            "whoami",
            "ps",
            "arp"
    };
    PrincipalInfo(FileWriter AlertFile,File hostDir, String name) throws IOException {
        fileWriterAlert = AlertFile;
        principalDumpDir = new File(hostDir,name);
        principalDumpDir.mkdirs();
        FileWriter fileWriter = new FileWriter(principalDumpDir.getPath()+"/net",false);
        writeTypeFileWriterHashMap.put(WriteType.net,fileWriter);
        fileWriter = new FileWriter(principalDumpDir.getPath()+"/file",false);
        writeTypeFileWriterHashMap.put(WriteType.file,fileWriter);
        fileWriter = new FileWriter(principalDumpDir.getPath()+"/subject",false);
        writeTypeFileWriterHashMap.put(WriteType.subject,fileWriter);
        fileWriterNode = new FileWriter(principalDumpDir.getPath()+"/subjectNode",false);
    }
    void dumpData(WriteType writeType,TCCDMDatum tccdmDatum) throws IOException {
        writeTypeFileWriterHashMap.get(writeType).write(tccdmDatum.toString()+'\n');
        writeTypeFileWriterHashMap.get(writeType).flush();
        Subject subject = (Subject)tccdmDatum.getDatum();
        String name = subject.getCmdLine()==null?"unknow":subject.getCmdLine().toString();
        if (subject2Number.get(name) == null) {
            fileWriterNode.write(name + ' ' + startNode.toString() + '\n');
            fileWriterNode.flush();
            subject2Number.put(name, startNode);
            //subjectEvent.put(startNode.toString(), new FileWriter(principalDumpDir.getPath() + '/' + startNode.toString(), false));
            ++startNode;
            for (String cmdline:E4CMDLine
            ) {
                if (name.equals(cmdline)){
                    fileWriterAlert.write(tccdmDatum.toString()+'\n');
                    fileWriterAlert.flush();
                }
            }
        }
    }

    private boolean compareEvent(Event e1, Event e2){
        if (e1.getSubject()==null ^ e2.getSubject()==null)
            return false;
        if (e1.getPredicateObject()==null ^ e2.getPredicateObject()==null)
            return false;
        if (e1.getPredicateObject2()==null ^ e2.getPredicateObject2()==null)
            return false;
        if (!e1.getSubject().equals(e2.getSubject()))
            return false;
        if (!e1.getPredicateObject().equals(e2.getPredicateObject()))
            return false;
        return e1.getPredicateObject2().equals(e2.getPredicateObject2());
    }

    void dumpData(String name, TCCDMDatum content) throws IOException {
        //subjectEvent.get(subject2Number.get(name).toString()).write(content.toString()+'\n');
    }
}

enum WriteType{
    net,
    file,
    ipc,
    subject,
    event,
    principal
}
