package MarpleNu.CDMLinuxFrameworkMain;

import java.util.concurrent.ConcurrentHashMap;

public class Node{
    ConcurrentHashMap<String, LabelForCS> labelList = new ConcurrentHashMap<>();
    int isOutput = 0;
    public String pcid = "" ;
    public static long num = 0;
    final long index = num;
    public Node() {
        num++;
    }

    public ConcurrentHashMap<String, LabelForCS> getLabelList() {
        return labelList;
    }
}
