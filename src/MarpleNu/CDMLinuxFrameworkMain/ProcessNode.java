package MarpleNu.CDMLinuxFrameworkMain;

import com.bbn.tc.schema.avro.cdm19.PrivilegeLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessNode extends Node{
    String processName = "";
    String cmdLine = "";
    int upid;
    public int pid = -1;
    long startTime = 0;
    long endTime = -1;
    public String subjectName="";
    PrivilegeLevel pLevel;
    //ConcurrentHashMap<FileNode, HashSet<Operator>> visitFile = new ConcurrentHashMap<>();
    ConcurrentHashMap<ProcessNode, HashSet<Operator>> visitProcess = new ConcurrentHashMap<>();
    ConcurrentHashMap<ProcessNode, HashSet<Operator>> visitedProcess = new ConcurrentHashMap<>();

    ProcessNode parentNode;      //父进程节�?
    ArrayList<ProcessNode> childNode = new ArrayList<>(); //子进程节点集�?
    ProcessNode(int uPid){
        upid = uPid;
    }
    ProcessNode(int Pid, int uPid){
    	pid = Pid;
        upid = uPid;
    }
    ProcessNode(String processname, int Pid, int uPid, long starttime){
        processName = processname;
        pid = Pid;
        upid = uPid;
        startTime = starttime;
    }
    ProcessNode(String processname, int Pid, int uPid, long starttime, long endtime){
        processName = processname;
        pid = Pid;
        upid = uPid;
        startTime = starttime;
        endTime = endtime;
    }
    ProcessNode(String processname, int Pid, int uPid, long starttime, PrivilegeLevel plevel){
        processName = processname;
        pid = Pid;
        upid = uPid;
        startTime = starttime;
        pLevel = plevel;
    }
    @Override
    public boolean equals(Object processNode){
        if (processNode == this) return true;
        if (!(processNode instanceof ProcessNode)) {
            return false;
        }
        ProcessNode p = (ProcessNode) processNode;
        return pcid.equals(p.pcid) && upid == p.upid && (startTime == p.startTime || startTime * p.startTime == 0);
    }
    @Override
    public int hashCode() {
        return (pcid + upid).hashCode();
    }

    public ProcessNode getParentNode() {
        return parentNode;
    }

    public ArrayList<ProcessNode> getChildNode() {
        return childNode;
    }
}
