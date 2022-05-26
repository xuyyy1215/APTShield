package MarpleNu.CDMLinuxFrameworkMain;

import java.util.ArrayList;

public class LabelForCS{
    String labelName = "";
    public int uuid = -1;
    public int pid = -1;
    public long timeStamp = -1;
    public String processNameOrFilePath = "";
    ArrayList<LabelForCS> sourceLabel = new ArrayList<>();
    ArrayList<String> sourceGrammar = new ArrayList<>();
    public String pcid = "trace";

    Node sourceNode;
    public LabelForCS(){}

    public LabelForCS(LabelForCS src){
        labelName = src.labelName;
        uuid = src.uuid;
        pid = src.pid;
        timeStamp = src.timeStamp;
        processNameOrFilePath = src.processNameOrFilePath;
        sourceLabel = src.sourceLabel;
        sourceGrammar = src.sourceGrammar;
        sourceNode = src.sourceNode;
        pcid = src.pcid;
    }

    public LabelForCS(String labelName) {
        this.labelName = labelName;
    }
    LabelForCS(String path, String labelname, long timestamp){
        this.processNameOrFilePath = path;
        this.labelName = labelname;
        this.timeStamp = timestamp;
    }
    public LabelForCS(String path, int Pid, String labelname, long timestamp, int uuid){
        this.processNameOrFilePath = path;
        this.pid = Pid;
        this.labelName = labelname;
        this.timeStamp = timestamp;
        this.uuid = uuid;
    }

    public LabelForCS(String labelName, int uuid, int pid, long timeStamp, String processNameOrFilePath) {
        this.labelName = labelName;
        this.pid = pid;
        this.uuid = uuid;
        this.timeStamp = timeStamp;
        this.processNameOrFilePath = processNameOrFilePath;
    }

    @Override
    public boolean equals(Object label){
        if (label == this) return true;
        if (!(label instanceof LabelForCS)) {
            return false;
        }
        LabelForCS l = (LabelForCS) label;
        if (processNameOrFilePath == null || l.processNameOrFilePath==null || labelName==null || l.labelName == null)
            return false;
        return processNameOrFilePath.equals(l.processNameOrFilePath) && labelName.equals(l.labelName) && uuid == l.uuid;
    }
    @Override
    public int hashCode() {
        return (processNameOrFilePath + labelName + uuid).hashCode();
    }

    @Override
    public String toString() {
        return "LabelForCS{" +
                "labelName='" + labelName + '\'' +
                ", uuid=" + uuid +
                ", pid=" + pid +
                ", timeStamp=" + timeStamp +
                ", processNameOrFilePath='" + processNameOrFilePath + '\'' +
                '}';
    }


    public ProcessNode getSourceNode() {
        return (ProcessNode)sourceNode;
    }

    public ArrayList<LabelForCS> getSourceLabel() {
        return sourceLabel;
    }

    public ArrayList<String> getSourceGrammar() {
        return sourceGrammar;
    }

    public String getLabelName() {
        return labelName;
    }
}
