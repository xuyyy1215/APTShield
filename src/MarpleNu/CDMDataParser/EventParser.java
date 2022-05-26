package MarpleNu.CDMDataParser;


import MarpleNu.CDMLinuxFrameworkMain.EventForCS;
import MarpleNu.CDMLinuxFrameworkMain.FileNode;
import MarpleNu.CDMLinuxFrameworkMain.ProcessNode;
import MarpleNu.FrameworkDataStruct.FrameworkFileInfo;
import MarpleNu.FrameworkDataStruct.FrameworkProcessInfo;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.Transform.LogUtil;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.avro.cdm19.UUID;
import org.apache.avro.generic.GenericFixed;
import scala.Int;
import scala.collection.parallel.ParIterableLike;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class EventParser extends BaseParser{
    private final SupportData supportData;
    //private HashMap<UUID,String> stashFileMap = new HashMap<>();
    public EventParser(SupportData supportData)
    {
        this.supportData = supportData;
    }
    // ConcurrentHashMap<String,FrameworkProcessInfo> fileReadReduce = new ConcurrentHashMap<>();
    private static final byte[] bt = {93, 64, -3, -5, 30, -124, 79, 22, -121, -12, 118, 106, -110, -14, 70, 15};
    private static final UUID tmp = new UUID(bt);
    /*by yjk*/
    private static final Map<UUID,UUID> clonelist = new HashMap<>();
    private static final Map<Integer, Integer> threadlist = new HashMap<>();
    public static Map<String, EventLeft> MapEventLeft = new HashMap<>();


    static long sum_net = 0;
    static long left_net = 0;
    static long sum_read = 0;
    static long left_read = 0;
    static long sum_write = 0;
    static long left_write = 0;

    static long sum_fork = 0;
    static long left_fork = 0;
    static long sum_clone = 0;
    static long left_clone = 0;
    static long sum_rename = 0;
    static long left_rename = 0;

    static long sum_event = 0;
    static long left_event = 0;
    /*by yjk*/
    static boolean pass=false;

    @Override
    public void parse(TCCDMDatum tccdmDatum)
    {
        sum_event ++;
        Event record = (com.bbn.tc.schema.avro.cdm19.Event) tccdmDatum.getDatum();
        /*by yjk*/
        //此处为什么要将非克隆的事件 和退出事件 单独进行
        if(!(record.getType().equals(EventType.EVENT_CLONE)||record.getType().equals(EventType.EVENT_EXIT))){
            record.setSubject(findSub(record.getSubject()));
            record.setThreadId(findThread(record.getThreadId()));
        }
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if(!fileName.equals("")){
            if(SupportData.fileStored.contains(fileName)) {
                supportData.test.getFileList().get("trace").put(fileName, FileNode.getFromDisk(fileName));
                SupportData.fileStored.remove(fileName);
            }
            if(supportData.test.getFileList().get("trace").containsKey(fileName)){
                supportData.test.getFileList().get("trace").get(fileName).setLatesttime();
            }
        }
        pass = false;
        /*by yjk*/
        switch (record.getType()){
            case EVENT_FORK: parseFork(record); break;
            case EVENT_CLONE: parseClone(record); break;
            case EVENT_EXECUTE: parseEventExecute(record); break; // ProcessStart
            case EVENT_EXIT: parseEventExit(record); break;  // ProcessEnd
            case EVENT_LOADLIBRARY: parseEventLoadLibrary(record); break; // ImageLoad
            case EVENT_SENDMSG: parseEventSendMsg(record); break; // Networkflow
            case EVENT_RECVMSG: parseEventRecvMsg(record); break; // Networkflow
            case EVENT_CONNECT: parseEventConnect(record); break; // ALPCALPC-Unwait, ALPCALPC-Wait-For-Reply, RegistryEnumerateKey, System call
            case EVENT_ACCEPT: parseEventAccept(record);break;
            case EVENT_WRITE: parseEventWrite(record); break; // DiskIoWrite
            case EVENT_READ: parseEventRead(record); break; // DiskIoWrite
            case EVENT_CREATE_OBJECT:parseCreateObject(record);break;
            case EVENT_RENAME:parseReName(record);break;
            case EVENT_MODIFY_FILE_ATTRIBUTES:parseModifyFileAttributes(record);break;
            case EVENT_UNLINK:parseUnlink(record);break;
            case EVENT_UPDATE:parseEventUpdate(record);break;
            case EVENT_OPEN: parseOpen(record);break;
            case EVENT_CLOSE: parseClose(record);break;
            /*by yjk*/
            case EVENT_MMAP: parseMmap(record);break;
            /*by yjk*/
            default: break;
        }
        if(MapEventLeft.size()>=5){
            MapEventLeft = new HashMap<>();
        }
        left_event = sum_event-left_net-left_rename-left_read-left_fork-left_clone-left_write;
        LogUtil logUtil = new LogUtil();

        LogUtil.update(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),""+sum_read,""+(sum_read-left_read),""+sum_write,""+(sum_write-left_write),""+sum_net,""+(sum_net-left_net),""+sum_fork,""+(sum_fork-left_fork),""+sum_clone,""+(sum_clone-left_clone),""+sum_rename,""+(sum_rename-left_rename),""+sum_event,""+left_event);
//        if(!pass)
//            LogUtil.insert(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), tccdmDatum.toString());
     }

    private void parseOpen(Event record) {
    }

    private void parseClose(Event record) {
    }
    private void parseEventUpdate(Event record)
    {
    }
    private void parseFork(Event record) {
        sum_fork++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)&&
                    temp.getEvent().equals("fork")){
                left_fork++;
                pass = true;
                return ;
            }
            else{
                EventLeft eventLeft = new EventLeft("fork",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("fork",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.ppid2ProcessMap.get(record.getThreadId());
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(2,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName()==null ?"":frameworkProcessInfo.getName(),
                frameworkProcessInfo.getCmdline()==null ?"":frameworkProcessInfo.getCmdline(),
                SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseClone(Event record) {
        /*by yjk*/
        putThread(record);
        /*by yjk*/
        sum_clone++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)&&
                    temp.getEvent().equals("clone")){
                left_clone++;
                pass = true;
                return ;
            }
            else{
                EventLeft eventLeft = new EventLeft("clone",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("clone",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }

    }

    private void parseReName(Event record)
    {
        /* yjk */
        sum_rename++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)&&
                    temp.getEvent().equals("rename")){
                left_rename++;
                pass = true;
                return ;
            }
            else{
                EventLeft eventLeft = new EventLeft("rename",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("rename",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(6,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseUnlink(Event record)
    {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(5,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseModifyFileAttributes(Event record)
    {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(8,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseCreateObject(Event record)
    {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        /*if (fileName!=""){
            stashFileMap.put(record.getPredicateObject(),fileName);
        }*/
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(7,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                null
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventLoadLibrary(Event record) {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS;
        if (frameworkProcessInfo.getName()!=null && fileName.contains(frameworkProcessInfo.getName())){
            eventForCS = new EventForCS(10,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        } else {
            eventForCS = new EventForCS(4,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        }
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventExit(Event record) {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(9,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName()==null ?"":frameworkProcessInfo.getName(),
                ""
        );

        supportData.test.addEvent(eventForCS);
        for (Iterator<Map.Entry<Integer, Integer>> it = threadlist.entrySet().iterator(); it.hasNext();){
            Map.Entry<Integer, Integer> item = it.next();
            if(item.getValue().equals(record.getThreadId()))
                it.remove();
        }

        for (Iterator<Map.Entry<UUID, UUID>> it = clonelist.entrySet().iterator(); it.hasNext();){
            Map.Entry<UUID, UUID> item = it.next();
            if(item.getValue().equals(record.getSubject()))
                it.remove();
        }

        ProcessNode p = supportData.test.getProcessList().get("trace").get(record.getThreadId());
        if(p==null){
            supportData.tid2ProcessMap.remove(record.getThreadId());
            return ;
        }
        boolean pd=true;
        for (Object o : p.getLabelList().entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            if (supportData.test.judgePHF((String)key)==1){
                pd = false;
            }
        }
        if(pd&&p.getChildNode().size()==0) {
            p.getParentNode().getChildNode().remove(p);
            supportData.test.getProcessList().get("trace").remove(record.getThreadId());
        }

        supportData.tid2ProcessMap.remove(record.getThreadId());
    }

    private void parseEventExecute(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        //System.out.println(record.getThreadId());
        EventForCS eventForCS = new EventForCS(3,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                frameworkProcessInfo.getCmdline() == null ? "" : frameworkProcessInfo.getCmdline(),
                SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventSendMsg(Event record) {
        /* yjk */
        sum_net++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)&&
                    temp.getEvent().equals("send")){
                left_net++;
                pass = true;
                return ;
            }
            else{
                EventLeft eventLeft = new EventLeft("send",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("send",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(1,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventRecvMsg(Event record) {
        /* yjk */
        sum_net++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)){
                if(temp.getEvent().equals("recv")){
                    left_net++;
                    pass = true;
                    return ;
                }else{
                    EventLeft eventLeft = new EventLeft("recv",processUUID,objectUUID,objectUUID2,time);
                    MapEventLeft.remove(processUUID);
                    MapEventLeft.put(processUUID,eventLeft);
                }
            }
            else{
                EventLeft eventLeft = new EventLeft("recv",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                for (Iterator<Map.Entry<String, EventLeft>> it = MapEventLeft.entrySet().iterator(); it.hasNext();){
                    Map.Entry<String, EventLeft> item = it.next();
                    if(item.getValue().getObjectID().equals(objectUUID)&&item.getValue().getObjectID2().equals(objectUUID2)&&
                            (item.getValue().getEvent().equals("send")))
                        it.remove();
                }
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("recv",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(0,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventConnect(Event record) {

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        if (frameworkFileInfo==null || frameworkProcessInfo==null)
            return;
        if (frameworkFileInfo.getProperty()==FrameworkFileInfo.socket)
            frameworkProcessInfo.setNetworkConnect(true);
    }

    private void parseEventAccept(Event record){

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        if (frameworkFileInfo==null || frameworkProcessInfo==null)
            return;
        if (frameworkFileInfo.getProperty()==FrameworkFileInfo.socket)
            frameworkProcessInfo.setNetworkConnect(true);
    }

    private void parseEventWrite(Event record) {
        /* yjk */
        sum_write++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)){
                if(temp.getEvent().equals("write")){
                    left_write++;
                    pass = true;
                    return ;
                }else{
                    EventLeft eventLeft = new EventLeft("write",processUUID,objectUUID,objectUUID2,time);
                    MapEventLeft.remove(processUUID);
                    MapEventLeft.put(processUUID,eventLeft);
                }
            }
            else{
                EventLeft eventLeft = new EventLeft("write",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                for (Iterator<Map.Entry<String, EventLeft>> it = MapEventLeft.entrySet().iterator(); it.hasNext();){
                    Map.Entry<String, EventLeft> item = it.next();
                    if(item.getValue().getObjectID().equals(objectUUID)&&item.getValue().getObjectID2().equals(objectUUID2)&&
                            (item.getValue().getEvent().equals("read")||item.getValue().getEvent().equals("loadelf")||item.getValue().getEvent().equals("mmap")))
                        it.remove();
                }
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("write",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        /*if (fileName==""){
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName==null) return ;
        }*/
        if (frameworkFileInfo!=null ||frameworkProcessInfo==null)
            return;
        //fileReadReduce.remove(fileName);
        EventForCS eventForCS = new EventForCS(1,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventRead(Event record) {
        /* yjk */
        sum_read++;
        String processUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String objectUUID = record.getPredicateObject() == null ? "":record.getPredicateObject().toString();
        String objectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        if(MapEventLeft.containsKey(processUUID)){
            EventLeft temp = MapEventLeft.get(processUUID);
            if(temp.getObjectID().equals(objectUUID)&&
                    temp.getObjectID2().equals(objectUUID2)&&
                    temp.getEvent().equals("read")){
                left_read++;
                pass = true;
                return ;
            }
            else{
                EventLeft eventLeft = new EventLeft("read",processUUID,objectUUID,objectUUID2,time);
                MapEventLeft.remove(processUUID);
                MapEventLeft.put(processUUID,eventLeft);
            }
        }
        else{
            EventLeft eventLeft = new EventLeft("read",processUUID,objectUUID,objectUUID2,time);
            MapEventLeft.put(processUUID,eventLeft);
        }
        /* yjk */
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        /*if (fileName==""){
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName==null) fileName = "";
        }*/
        if (frameworkProcessInfo==null)
            return;
        /*if (fileReadReduce.containsKey(fileName)){
            if (fileReadReduce.get(fileName).equals(frameworkProcessInfo))
                return;
            else
                fileReadReduce.replace(fileName,frameworkProcessInfo);
        } else{
            fileReadReduce.put(fileName,frameworkProcessInfo);
        }*/
        EventForCS eventForCS = new EventForCS(0,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }
    /*by yjk*/
    private void putThread(Event record){
        UUID pSub = record.getSubject();
        UUID cObject = record.getPredicateObject();
        clonelist.put(cObject,pSub);

        FrameworkProcessInfo frameworkProcessInfo = supportData.ppid2ProcessMap.get(record.getThreadId());
        int cThread = frameworkProcessInfo.getTgid();
        int pThread = frameworkProcessInfo.getPpid();
        threadlist.put(cThread,pThread);
    }
    private UUID findSub(UUID thread){
        UUID result = thread;
        while(thread != null){
            result = thread;
            thread = clonelist.get(result);
        }
        return result;
    }
    private Integer findThread(Integer thread){
        Integer result = thread;
        while(thread != null) {
            result = thread;
            thread = threadlist.get(result);
        }
        return result;
    }

    private void parseMmap(Event record){

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        /*if (fileName==""){
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName==null) fileName = "";
        }*/
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(15,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }
    /*by yjk*/
}
