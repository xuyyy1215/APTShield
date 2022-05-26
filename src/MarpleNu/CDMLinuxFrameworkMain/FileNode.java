package MarpleNu.CDMLinuxFrameworkMain;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class FileNode extends Node {
    String filePath;
    ArrayList<String> originFilePath = new ArrayList<>();
    ConcurrentHashMap<ProcessNode, HashSet<Operator>> visitedProcess  = new ConcurrentHashMap<>();
    FileNode(String filepath){
        filePath = filepath;setLatesttime();
    }
    long latesttime = 0;


    public boolean equals(Object fileNode){
        if (fileNode == this) return true;
        if (!(fileNode instanceof LabelForCS)) {
            return false;
        }
        FileNode f = (FileNode) fileNode;
        return filePath.equals(f.filePath) && pcid.equals(f.pcid);
    }
    @Override
    public int hashCode() {
        return (filePath + pcid).hashCode();
    }

    public ConcurrentHashMap<ProcessNode, HashSet<Operator>> getVisitedProcess() {
        return visitedProcess;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setLatesttime() {
        this.latesttime = System.currentTimeMillis();
    }

    public long getLatesttime() {
        return latesttime;
    }

    public static FileNode getFromDisk(String filename){
        return new FileNode(filename);
    }

}
