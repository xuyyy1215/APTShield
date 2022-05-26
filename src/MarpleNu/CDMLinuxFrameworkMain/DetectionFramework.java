package MarpleNu.CDMLinuxFrameworkMain;

import MarpleNu.CDMDataClassifier.CDMDataClassifier;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import MarpleNu.Transform.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;


public class DetectionFramework {

    private static final Logger logger = LoggerFactory.getLogger(DetectionFramework.class);
    private static final HashMap<String, DetectionFramework> clientId2Instance = new HashMap<>();
    private static final HashMap<String, Integer> judgeMode2AlertLevel = new HashMap<>();
    public static DetectionFramework getInstance(String clientId) {
        DetectionFramework instance = clientId2Instance.get(clientId);
        if (instance == null) {
            instance = new DetectionFramework(clientId);
            clientId2Instance.put(clientId, instance);
        }
        return instance;
    }
    //
    public static void removeClient(String clientId) {
        clientId2Instance.remove(clientId);
    }
    private final ArrayList<ArrayList<ArrayList<String>>> transGrammar = new ArrayList<>();	//transmit grammar structure
    private final ArrayList<ArrayList<String>> labelGrammar = new ArrayList<>();	//label grammar structure
    private final LinkedHashMap<String, ArrayList<ArrayList<String>>> judgeGrammar = new LinkedHashMap<>();	//judge output grammar structure
    private final ArrayList<ArrayList<String>> eventGrammar = new ArrayList<>();	//event structure

    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ArrayList<ProcessNode>>> ProcessHangingPortList = new ConcurrentHashMap<>(); //record the s-process who link to this d-port but we don't know who listen to this d-port, when get the d-process from the PortList, we clear this list.
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ProcessNode>> PortList = new ConcurrentHashMap<>(); //record the process who listens to the port

    private final ConcurrentHashMap<String, HashSet<String>> IpList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, FileNode>> FileList = new ConcurrentHashMap<>(); //the file list
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ProcessNode>> ProcessList = new ConcurrentHashMap<>(); //use the process uid as key, only consider the process which is on.
    private final ConcurrentHashMap<String, ProcessNode> ProcessRoot = new ConcurrentHashMap<>();//ProcessNode("", 0, 0, 0);   //the root of the process tree.
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, ProcessNode>> TotalStartProcess = new ConcurrentHashMap<>();
    //    private ConcurrentHashMap<Long, ProcessNode> closeProcess = new ConcurrentHashMap<Long, ProcessNode>();

    /*by yjk*/

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, ProcessNode>> getProcessList(){
        return ProcessList;
    }

    public ConcurrentHashMap<String, ProcessNode> getProcessRoot() {
        return ProcessRoot;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, FileNode>> getFileList() {
        return FileList;
    }

    private final HashMap<Integer,List<String>> outputList = new HashMap<Integer,List<String>>();
    public boolean isOutput(int pid,String judgemode){
        if(outputList.get(pid)==null){
            outputList.put(pid,new ArrayList<String>());
            outputList.get(pid).add(judgemode);
            return true;
        }
        else if(outputList.get(pid).contains(judgemode)){
            return false;
        }
        else{
            outputList.get(pid).add(judgemode);
            return true;
        }
    }
    /*by yjk*/
    private final String clientId;
    private HashMap<String,String> kafkaConfigMap =  new HashMap<>();
    private String topic = "";
    private DetectionFramework(String clientId){
        String kafkaPathKey = "kafka_conf_path";
        String path = FrameworkConfig.Search(kafkaPathKey);
        try {
            kafkaConfigMap = FrameworkConfig.buildmap(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        topic = kafkaConfigMap.get("topic");
        this.clientId = clientId;
        try{
            setGrammar();
        }
        catch(IOException e){
            logger.info("set grammar error", e);
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                outputProcessDotAndFileCsv();
            }
        };
        TimerTask readCommand = new TimerTask() {
            @Override
            public void run() {
                File file = new File("Command/command.txt");
                if (file.exists()) {
                    try (Scanner scanner = new Scanner(new FileReader(file))) {
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            if ("draw".equals(line)) {
                                outputProcessDotAndFileCsv();
                            } else if (line.contains("query")) {
                                String pcid = line.split(" ")[1];
                                int pid = Integer.parseInt(line.split(" ")[2]);
                                long timestamp = Long.parseLong(line.split(" ")[3]);
                                int maxDepth = Integer.parseInt(line.split(" ")[4]);
                                chooseProcessRelationToDraw(pcid, pid, timestamp, maxDepth);
                            } else if (line.contains(" ")) {
                                String[] splitted = line.split(" ");
                                String pcid = splitted[0];
                                int pid = Integer.parseInt(splitted[1]);
                                long timestamp = Long.parseLong(splitted[2]);
                                chooseProcessToDraw(pcid, pid, timestamp);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try (PrintWriter pw = new PrintWriter(new FileWriter(file))){
                        pw.print("");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 10000, 1800000);
        Timer timer1 = new Timer();
        timer1.scheduleAtFixedRate(readCommand, 10000, 10000);
    }
    private void setGrammar()throws IOException{
        //set the policy how to transmit the label
        Scanner scanner = new Scanner(new FileReader("trans.txt"));
        String tempString;
        while (scanner.hasNextLine()) {
            tempString = scanner.nextLine();
            if ("".equals(tempString)) {
                continue;
            }
            transGrammar.add(new ArrayList<>());
            String []list = tempString.split(",");
            transGrammar.get(transGrammar.size()-1).add(new ArrayList<>(Collections.singletonList(tempString)));
            for (String aList : list) {
                transGrammar.get(transGrammar.size() - 1).add(new ArrayList<>(Arrays.asList(aList.split("/"))));
            }
        }
        scanner.close();
        scanner = new Scanner(new FileReader("label.txt"));
        for(int i = 0; i <= 4; i++){
            labelGrammar.add(new ArrayList<>());
        }
        while (scanner.hasNextLine()) {
            tempString = scanner.nextLine();
            if ("".equals(tempString)) {
                continue;
            }
            String []list = tempString.split(",");
            for(int i = 0; i <= 4; i++){
                labelGrammar.get(i).add(list[i]);
            }
        }
        scanner.close();
        scanner = new Scanner(new FileReader("judge.txt"));
        while (scanner.hasNextLine()) {
            tempString = scanner.nextLine();
            if ("".equals(tempString)) {
                continue;
            }
            String name = tempString;
            judgeGrammar.put(name, new ArrayList<>());
            while (scanner.hasNextLine()) {
                tempString = scanner.nextLine();
                if ("".equals(tempString)) {
                    continue;
                }
                if ("0".equals(tempString)) {
                    break;
                }
                judgeGrammar.get(name).add(new ArrayList<>());
                String []list = tempString.split("/");
                judgeGrammar.get(name).get(judgeGrammar.get(name).size()-1).addAll(Arrays.asList(list));
            }
        }
        scanner.close();
        scanner = new Scanner(new FileReader("event.txt"));
        for(int i = 0; i <= 1; i++){
            eventGrammar.add(new ArrayList<>());
        }
        while (scanner.hasNextLine()) {
            tempString = scanner.nextLine();
            if ("".equals(tempString)) {
                continue;
            }
            String []list = tempString.split(",");
            for(int i = 0; i <= 1; i++){
                eventGrammar.get(i).add(list[i]);
            }
        }
        scanner.close();
        int i = 1;
        for(String judgeMode : judgeGrammar.keySet()){
            judgeMode2AlertLevel.put(judgeMode, i);
            i++;
        }
    }
    public void addEvent(EventForCS event){
        //logger.info("{} add event {}", clientId, event);
        //add the event into the list
        if(!ProcessList.containsKey(event.pcid)){
            ProcessHangingPortList.put(event.pcid, new ConcurrentHashMap<>());
            PortList.put(event.pcid, new ConcurrentHashMap<>());
            IpList.put(event.pcid, new HashSet<>());
            FileList.put(event.pcid, new ConcurrentHashMap<>());
            ProcessList.put(event.pcid, new ConcurrentHashMap<>());
            ProcessRoot.put(event.pcid, new ProcessNode("", 0, 0, 0));
            TotalStartProcess.put(event.pcid, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<Integer, ProcessNode> processList = ProcessList.get(event.pcid);
        ConcurrentHashMap<String, FileNode> fileList = FileList.get(event.pcid);
        ProcessNode processRoot = ProcessRoot.get(event.pcid);
        ConcurrentHashMap<Long, ProcessNode> totalStartProcess = TotalStartProcess.get(event.pcid);

        if (event.processNameOrFilePath != null) {
            event.processNameOrFilePath = event.processNameOrFilePath.toLowerCase();
        }
        if (event.processNameOrFilePath2 != null) {
            event.processNameOrFilePath2 = event.processNameOrFilePath2.toLowerCase();
        }
        if(event.eventType == 2||event.eventType==3||event.eventType==13){
            ProcessNode newNode;
            if(processList.containsKey(event.uuid)){
                newNode = processList.get(event.uuid);
                if(newNode.parentNode != null){
                    newNode.parentNode.childNode.remove(newNode);
                }
                newNode.pid = event.pid;
                newNode.processName = event.processNameOrFilePath;
                newNode.startTime = event.timestamp;
                newNode.cmdLine = event.processNameOrFilePath2;
                newNode.pcid = event.pcid;
                newNode.pLevel = event.pLevel;
                newNode.subjectName = event.subjectName;
            }
            else{
                newNode = new ProcessNode(event.processNameOrFilePath, event.pid, event.uuid, event.timestamp, event.pLevel);
                newNode.cmdLine = event.processNameOrFilePath2;
                newNode.pcid = event.pcid;
                newNode.subjectName = event.subjectName;
                processList.put(event.uuid, newNode);
            }
            ProcessNode parentNode = processList.get(event.parentUuid);
            if(parentNode == null){
                parentNode = new ProcessNode(event.parentUuid);
                parentNode.processName = "parentNeverShowUp";
                parentNode.parentNode = processRoot;
                parentNode.pcid = event.pcid;
                processRoot.childNode.add(parentNode);
                processList.put(event.parentUuid, parentNode);
            }
            totalStartProcess.put(newNode.pid + newNode.startTime, newNode);
            if (event.eventType==2) {
                for (Object o : parentNode.labelList.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    Object key = entry.getKey();
                    if(key.equals("PT10")||
                            key.equals("PT9")||
                            key.equals("PT4")||
                            key.equals("PT5")||
                            key.equals("PT6")){
                        continue;
                    }
                    Object val = entry.getValue();

                    LabelForCS labelForCS = new LabelForCS((LabelForCS) val);
                    newNode.labelList.put((String) key, labelForCS);
                }
            }
            if(event.parentUuid != event.uuid){
                parentNode.childNode.add(newNode);
                newNode.parentNode = parentNode;
                transLabel(parentNode, newNode, event.eventType, transGrammar, 2);
            }
        }
        else if(event.eventType == 9){
            ProcessNode Node = processList.get(event.uuid);
            if(Node == null){
                //System.out.println("eventtype" + event.eventType + ": not found process with pid " + event.pid);
                return;
            }
            Node.endTime = event.timestamp;
//            closeProcess.put(Node.pid + Node.endTime, Node);
            //processList.remove(event.uuid);
        }
        else if(event.eventType==10 || event.eventType == 0 || event.eventType == 1
                || event.eventType == 4 || event.eventType == 6 || event.eventType == 7 || event.eventType == 8
                || event.eventType == 11 || event.eventType == 12 || event.eventType == 14
                /*by yjk*/
                || event.eventType == 15
                /*by yjk*/
        ){
            ProcessNode Node;
            if(processList.containsKey(event.uuid)){
                Node = processList.get(event.uuid);
            }
            else{
                Node = new ProcessNode(event.pid, event.uuid);
                Node.pcid = event.pcid;
                processList.put(event.uuid, Node);
                /* yjk */
                ProcessNode parentNode = processList.get(event.parentUuid);
                if(parentNode==null){
                    if(event.parentUuid!=1){
                        ProcessNode pNode= new ProcessNode("", event.parentUuid, event.parentUuid, 0);
                        pNode.parentNode = processRoot;
                        Node.parentNode = pNode;
                        pNode.childNode.add(Node);
                        processRoot.childNode.add(pNode);
                        processList.put(event.parentUuid, pNode);
                    }
                    else{
                        Node.parentNode = processRoot;
                        processRoot.childNode.add(Node);
                    }
                }
                else{
                    Node.parentNode = parentNode;
                    parentNode.childNode.add(Node);
                }
                /* yjk */
            }
            FileNode file;
            if(fileList.containsKey(event.processNameOrFilePath)){
                file = fileList.get(event.processNameOrFilePath);
            }
            else{
                file = new FileNode(event.processNameOrFilePath);
                file.pcid = event.pcid;
                fileList.put(file.filePath, file);
            }
            if(event.eventType == 5){
                if(event.processNameOrFilePath.equals(event.processNameOrFilePath2)){
                    return;
                }
                fileList.remove(event.processNameOrFilePath2);
                fileList.put(event.processNameOrFilePath2, file);
                file.originFilePath.add(file.filePath);
                file.filePath = event.processNameOrFilePath2;
            }

            else {

                Operator operate = new Operator(event.eventType, event.timestamp);
                if (!file.visitedProcess.containsKey(Node)) {
                    file.visitedProcess.put(Node, new HashSet<>());
                }
                /*
                file.visitedProcess.get(Node).add(operate);
                if (!Node.visitFile.containsKey(file)) {
                    Node.visitFile.put(file, new HashSet<>());
                }
                Node.visitFile.get(file).add(operate);
                */
                if(!file.filePath.equals("")){
                    transLabel(Node, file, event.eventType, transGrammar, 2);
                }
            }
        }
        /*
        else if(event.eventType == 10){
            ProcessNode Node = processList.get(event.uuid);
            if(Node == null){
                Node = new ProcessNode(event.pid, event.uuid);
                Node.pcid = event.pcid;
                processList.put(event.uuid, Node);
                Node.parentNode = processRoot;
                processRoot.childNode.add(Node);
            }
            Node.processName = event.processNameOrFilePath;
            FileNode file;
            if(fileList.containsKey(event.processNameOrFilePath)){
                file = fileList.get(event.processNameOrFilePath);
            }
            else{
                file = new FileNode(event.processNameOrFilePath);
                file.pcid = event.pcid;
                fileList.put(file.filePath, file);
            }
            Operator operate = new Operator(event.eventType, event.timestamp);
            if(!file.visitedProcess.containsKey(Node)){
                file.visitedProcess.put(Node, new HashSet<>());
            }
            file.visitedProcess.get(Node).add(operate);
            if(!Node.visitFile.containsKey(file)){
                Node.visitFile.put(file, new HashSet<>());
            }
            Node.visitFile.get(file).add(operate);
            transLabel(Node, file, event.eventType, transGrammar, 2);
        }
        */
        /*
        else if(event.eventType == 5 || event.eventType == 7){
            ProcessNode parentNode;
            if(processList.containsKey(event.parentUuid)){
                parentNode = processList.get(event.parentUuid);
            }
            else{
                parentNode = new ProcessNode(event.parentUuid);
                parentNode.pcid = event.pcid;
                processList.put(event.parentUuid, parentNode);
                parentNode.parentNode = processRoot;
                processRoot.childNode.add(parentNode);
            }
            ProcessNode sonNode;
            if(processList.containsKey(event.uuid)){
                sonNode = processList.get(event.uuid);
            }
            else{
                sonNode = new ProcessNode(event.pid, event.uuid);
                sonNode.pcid = event.pcid;
                processList.put(event.uuid, sonNode);
                sonNode.parentNode = processRoot;
                processRoot.childNode.add(sonNode);
            }
            Operator operate = new Operator(event.eventType, event.timestamp);
            if(!sonNode.visitedProcess.containsKey(parentNode)){
                sonNode.visitedProcess.put(parentNode, new HashSet<>());
            }
            sonNode.visitedProcess.get(parentNode).add(operate);
            if(!parentNode.visitProcess.containsKey(sonNode)){
                parentNode.visitProcess.put(sonNode, new HashSet<>());
            }
            parentNode.visitProcess.get(sonNode).add(operate);
//            if(event.eventType == 5){
//                LabelForCS l = new LabelForCS(sonNode.processName, sonNode.pid, "PT17", event.timestamp, sonNode.upid);
//                addLabel(l);
//            }
            transLabel(parentNode, sonNode, event.eventType, transGrammar, 2);
        }
        */
        /*
        else if(event.eventType == 10 || event.eventType == 11 || event.eventType == 12 || event.eventType == 13){
            ProcessNode parentNode;
            ProcessNode sonNode;
            if(processList.containsKey(event.uuid)){
                parentNode = processList.get(event.uuid);
            }
            else{
                parentNode = new ProcessNode(event.uuid);
                parentNode.pcid = event.pcid;
                processList.put(event.uuid, parentNode);
                parentNode.parentNode = processRoot;
                processRoot.childNode.add(parentNode);
            }
        	PortList.get(event.pcid).put(event.sport, parentNode);
        	if(ProcessHangingPortList.get(event.pcid).containsKey(event.sport) && ProcessHangingPortList.get(event.pcid).get(event.sport).size() > 0){
        		for(ProcessNode hangParentNode : ProcessHangingPortList.get(event.pcid).get(event.sport)){
        			Operator operate = new Operator(event.eventType, event.timestamp);
                    if(!parentNode.visitedProcess.containsKey(hangParentNode)){
                    	parentNode.visitedProcess.put(hangParentNode, new HashSet<>());
                    }
                    parentNode.visitedProcess.get(hangParentNode).add(operate);
                    if(!hangParentNode.visitProcess.containsKey(parentNode)){
                    	hangParentNode.visitProcess.put(parentNode, new HashSet<>());
                    }
                    hangParentNode.visitProcess.get(parentNode).add(operate);
                    transLabel(hangParentNode, parentNode, event.eventType, transGrammar, 2);
        		}
        		ProcessHangingPortList.get(event.pcid).get(event.sport).clear();
        	}
            if(event.saddr.equals("127.0.0.1") || event.saddr.equals("0.0.0.0")){
            	if(PortList.get(event.pcid).containsKey(event.dport)){
            		sonNode = PortList.get(event.pcid).get(event.dport);
                    Operator operate = new Operator(event.eventType, event.timestamp);
                    if(!sonNode.visitedProcess.containsKey(parentNode)){
                        sonNode.visitedProcess.put(parentNode, new HashSet<>());
                    }
                    sonNode.visitedProcess.get(parentNode).add(operate);
                    if(!parentNode.visitProcess.containsKey(sonNode)){
                        parentNode.visitProcess.put(sonNode, new HashSet<>());
                    }
                    parentNode.visitProcess.get(sonNode).add(operate);
                    transLabel(parentNode, sonNode, event.eventType, transGrammar, 2);
            	}
            	else{
            		if(!ProcessHangingPortList.get(event.pcid).containsKey(event.dport)){
            			ProcessHangingPortList.get(event.pcid).put(event.dport, new ArrayList<>());
            		}
            		ProcessHangingPortList.get(event.pcid).get(event.dport).add(parentNode);
            	}
            }
            else{
                IpList.get(event.pcid).add(event.saddr);
                for(String pcID : IpList.keySet()){
                	if(IpList.get(pcID).contains(event.daddr)){
                    	if(PortList.get(pcID).containsKey(event.dport)){
                    		sonNode = PortList.get(pcID).get(event.dport);
                            Operator operate = new Operator(event.eventType, event.timestamp);
                            if(!sonNode.visitedProcess.containsKey(parentNode)){
                                sonNode.visitedProcess.put(parentNode, new HashSet<>());
                            }
                            sonNode.visitedProcess.get(parentNode).add(operate);
                            if(!parentNode.visitProcess.containsKey(sonNode)){
                                parentNode.visitProcess.put(sonNode, new HashSet<>());
                            }
                            parentNode.visitProcess.get(sonNode).add(operate);
                            transLabel(parentNode, sonNode, event.eventType, transGrammar, 2);
                    	}
                    	else{
                    		if(!ProcessHangingPortList.get(pcID).containsKey(event.dport)){
                    			ProcessHangingPortList.get(pcID).put(event.dport, new ArrayList<>());
                    		}
                    		ProcessHangingPortList.get(pcID).get(event.dport).add(parentNode);
                    	}
                	}
                }
            }
        }*/
    }
    public void addLabel(LabelForCS l){
        //logger.info("{} add label {}", clientId, l);
        //set the label into the list
        //System.out.printf("%s %d %s\n",l.labelName,l.pid,l.processNameOrFilePath);
        if(!ProcessList.containsKey(l.pcid)){
            IpList.put(l.pcid, new HashSet<>());
            ProcessHangingPortList.put(l.pcid, new ConcurrentHashMap<>());
            PortList.put(l.pcid, new ConcurrentHashMap<>());
            FileList.put(l.pcid, new ConcurrentHashMap<>());
            ProcessList.put(l.pcid, new ConcurrentHashMap<>());
            ProcessRoot.put(l.pcid, new ProcessNode("", 0, 0, 0));
            TotalStartProcess.put(l.pcid, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<Integer, ProcessNode> processList = ProcessList.get(l.pcid);
        ConcurrentHashMap<String, FileNode> fileList = FileList.get(l.pcid);
        ProcessNode processRoot = ProcessRoot.get(l.pcid);

        if (l.processNameOrFilePath != null) {
            l.processNameOrFilePath = l.processNameOrFilePath.toLowerCase();
        }
        if(l.pid == -1){
            assert l.processNameOrFilePath != null;
            FileNode file = fileList.get(l.processNameOrFilePath);
            if(file == null){
                file = new FileNode(l.processNameOrFilePath);
                file.pcid = l.pcid;
                fileList.put(l.processNameOrFilePath, file);
            }
            if(file.labelList.containsKey(l.labelName)){
                if(file.labelList.get(l.labelName).timeStamp > l.timeStamp){
                    file.labelList.get(l.labelName).timeStamp = l.timeStamp;
                    return;
                }
            }
            else{
                file.labelList.put(l.labelName, l);
                l.sourceNode = file;
            }
            for(ProcessNode Node : file.visitedProcess.keySet()){
                for(Operator o : file.visitedProcess.get(Node)){
                    transLabel(Node, file, o.operate, transGrammar, 1);
                }
            }
        }
        else{
            ProcessNode Node = processList.get(l.pid);
            if(Node == null){
                Node = new ProcessNode(l.pid, l.pid);
                Node.pcid = l.pcid;
                processList.put(l.pid, Node);
                /* yjk */
                ProcessNode parentNode = processList.get(l.uuid);
                if(parentNode==null){
                    Node.parentNode = processRoot;
                    processRoot.childNode.add(Node);
                }
                else {
                    Node.parentNode = parentNode;
                    parentNode.childNode.add(Node);
                    for (Object o : parentNode.labelList.entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        Object key = entry.getKey();
                        if (key.equals("PT7") ||
                                key.equals("PT8") ||
                                key.equals("PT4")) {
                            Object val = entry.getValue();
                            LabelForCS labelForCS = new LabelForCS((LabelForCS) val);
                            Node.labelList.put((String) key, labelForCS);
                        }

                    }
                }
                /* yjk */
            }
            if(Node.labelList.containsKey(l.labelName)){
                if(Node.labelList.get(l.labelName).timeStamp > l.timeStamp){
                    Node.labelList.get(l.labelName).timeStamp = l.timeStamp;
                    return;
                }
            }
            else{
                Node.labelList.put(l.labelName, l);
                l.sourceNode = Node;
            }

            /*by yjk*/
            if(judgePHF(l.labelName) == 1){
                judgeOutput(Node);
            }
            /*by yjk*/
            if(Node.parentNode != null){
                transLabel(Node.parentNode, Node, 0, transGrammar, 1);  //逆向
            }
            for(ProcessNode parentNode : Node.visitedProcess.keySet()){
                for(Operator o : Node.visitedProcess.get(parentNode)){
                    transLabel(parentNode, Node, o.operate, transGrammar, 1);  //逆向
                }
            }
        }
    }

    private void transLabel(Node fatherNode, Node sonNode, int type, ArrayList<ArrayList<ArrayList<String>>> transGrammar, int directOrReverse){
        //label transmit grammar. Need time stamp to judge the time between event and label 0: direct, 1:reverse 2:both
        if(directOrReverse % 2 ==0){
            for(String label : fatherNode.labelList.keySet()){
                for(ArrayList<ArrayList<String>> grammar: transGrammar){
                    if(grammar.get(4).get(0).equals("D") && grammar.get(1).contains(label) && grammar.get(2).contains(String.valueOf(type))){
                        transSingleLabel(fatherNode, sonNode, "D", type, label, grammar.get(3).get(0), grammar.get(0).get(0));
                    }
                }
            }
        }
        if(directOrReverse > 0){
            for(String label : sonNode.labelList.keySet()){
                for(ArrayList<ArrayList<String>> grammar: transGrammar){
                    if(grammar.get(4).get(0).equals("R") && grammar.get(3).contains(label) && grammar.get(2).contains(String.valueOf(type))){
                        transSingleLabel(fatherNode, sonNode, "R", type, grammar.get(1).get(0), label, grammar.get(0).get(0));
                    }
                }
            }
        }
    }

    private void transSingleLabel(Node Node_0, Node Node_1, String directOrReverse, int type, String label_0, String label_1, String grammar){
        if(Node_0 instanceof FileNode){
            return;
        }
        /* yjk */
        /* yjk */
        //ProcessNode p1 = (ProcessNode)Node_0;
        /*
        System.out.printf("%s %s\n",label_0,label_1);
        if (Node_0 instanceof ProcessNode){
            System.out.printf("%s %d %s\n",((ProcessNode) Node_0).processName, ((ProcessNode) Node_0).pid,((ProcessNode) Node_0).cmdLine);
        }
        System.out.printf("%s %s\n",label_0,label_1);
        if (Node_1 instanceof ProcessNode){
            System.out.printf("%s %d %s\n",((ProcessNode) Node_1).processName, ((ProcessNode) Node_1).pid,((ProcessNode) Node_1).cmdLine);
        }
        */
        LabelForCS l;
        if(directOrReverse.equals("D")){
            if(judgePHF(label_0) == 1){
                judgeOutput(Node_0);
            }
            if(label_1.equals("=")){
                if(Node_1 instanceof FileNode){
                    FileNode son = (FileNode)Node_1;
                    l = new LabelForCS(son.filePath, label_0, Node_0.labelList.get(label_0).timeStamp);
                }
                else{
                    ProcessNode son = (ProcessNode)Node_1;
                    l = new LabelForCS(son.processName, son.pid, label_0, Node_0.labelList.get(label_0).timeStamp, son.upid);
                }
            }
            else{
                if(Node_1 instanceof FileNode){
                    FileNode son = (FileNode)Node_1;
                    l = new LabelForCS(son.filePath, label_1, Node_0.labelList.get(label_0).timeStamp);
                }
                else{
                    ProcessNode son = (ProcessNode)Node_1;
                    l = new LabelForCS(son.processName, son.pid, label_1, Node_0.labelList.get(label_0).timeStamp, son.upid);
                }
            }/* yjk */
            if(label_1.equals("FT11")){
                FileNode node1 =(FileNode) Node_1;
                if((node1.getFilePath().startsWith("/dev/")) ||
                        (node1.getFilePath().startsWith("/var/")) ||
                        (node1.getFilePath().contains(".bash_history"))
                ){
                    return ;
                }

            }

            /* yjk */
            if(!Node_1.labelList.containsKey(l.labelName)){
                Node_1.labelList.put(l.labelName, l);
                l.sourceNode = Node_1;
                if(!l.sourceLabel.contains(Node_0.labelList.get(label_0))){
                    l.sourceLabel.add(Node_0.labelList.get(label_0));
                    l.sourceGrammar.add(grammar);
                }
                outputLog(Node_0, Node_1, type, Node_0.labelList.get(label_0), l, grammar);  //防止重复传�??
                if(judgePHF(l.labelName) == 1){
                    judgeOutput(Node_1);
                }
            }
            else{
                if(Node_1.labelList.get(l.labelName).timeStamp > l.timeStamp){
                    Node_1.labelList.get(l.labelName).timeStamp = l.timeStamp;
                }
                if(!Node_1.labelList.get(l.labelName).sourceLabel.contains(Node_0.labelList.get(label_0))){
                    Node_1.labelList.get(l.labelName).sourceLabel.add(Node_0.labelList.get(label_0));
                    Node_1.labelList.get(l.labelName).sourceGrammar.add(grammar);
                }
            }

        }
        else{
            if(judgePHF(label_1) == 1){
                judgeOutput(Node_1);
            }
            ProcessNode father = (ProcessNode)Node_0;
            if(label_0.equals("=")){
                l = new LabelForCS(father.processName, father.pid, label_1, Node_1.labelList.get(label_1).timeStamp, father.upid);
            }
            else{
                l = new LabelForCS(father.processName, father.pid, label_0, Node_1.labelList.get(label_1).timeStamp, father.upid);
            }
            if(!father.labelList.containsKey(l.labelName)){
                father.labelList.put(l.labelName, l);
                l.sourceNode = father;
                if(!l.sourceLabel.contains(Node_1.labelList.get(label_1))){
                    l.sourceLabel.add(Node_1.labelList.get(label_1));
                    l.sourceGrammar.add(grammar);
                }
                outputLog(Node_0, Node_1, type, l, Node_1.labelList.get(label_1), grammar);  //防止重复传�??
                if(judgePHF(l.labelName) == 1){
                    judgeOutput(Node_0);
                }
                if(father.parentNode != null && l.labelName.equals(label_1)){
                    transSingleLabel(father.parentNode, father, "R", type, "=", label_1, grammar); //递归传�??
                }
                if(father.parentNode !=null && (l.labelName.equals("PS3")||l.labelName.equals("PS4"))){
                    father.parentNode.labelList.put(l.labelName,l);
                }
            }
            else{
                if(Node_0.labelList.get(l.labelName).timeStamp > l.timeStamp){
                    Node_0.labelList.get(l.labelName).timeStamp = l.timeStamp;
                }
                if(!Node_0.labelList.get(l.labelName).sourceLabel.contains(Node_1.labelList.get(label_1))){
                    Node_0.labelList.get(l.labelName).sourceLabel.add(Node_1.labelList.get(label_1));
                    Node_0.labelList.get(l.labelName).sourceGrammar.add(grammar);
                }
            }
            /* yjk */
            if(label_1.equals("FT7")){
                Node node1 = Node_0;
                while(((ProcessNode) node1).parentNode!=null&&((ProcessNode) node1).parentNode.pid!=0)
                {
                    node1 = ((ProcessNode) node1).parentNode;
                }
                ProcessNode node11 = (ProcessNode)node1;
                if(!node11.labelList.containsKey(l.labelName)){
                    l = new LabelForCS(node11.processName, father.upid, label_0, Node_0.labelList.get(label_0).timeStamp, father.upid);
                    node11.labelList.put(l.labelName, l);
                    l.sourceNode = father;
                }
                for(ProcessNode tempNode:node11.childNode){
                    if(!tempNode.labelList.containsKey(l.labelName)) {
                        l = new LabelForCS(tempNode.processName, father.pid, label_0, Node_0.labelList.get(label_0).timeStamp, father.upid);
                        tempNode.labelList.put(l.labelName, l);
                        l.sourceNode = father;
                    }
                }
            }
            /* yjk */
        }
    }

    public int judgePHF(String label){
        ArrayList<String> phfList = new ArrayList<>();
        for(int i = 0; i < labelGrammar.get(0).size(); i++){
            if(labelGrammar.get(3).get(i).equals("PHF")){
                phfList.add(labelGrammar.get(0).get(i));
            }
        }
        if(phfList.contains(label)){
            return 1;
        }
        else{
            return 0;
        }
    }
    private void judgeOutput(Node node){
        // judge whether it is a malicious process
        int sum;
        double maliciousScore = 0, confidenceScore;
        double phfScore = 0, codeSourceScore = 0, featureScore = 0, trueScore = 1;
        /*if(node.isOutput == 3){
            return;
        }*/
        for(String label : node.labelList.keySet()){
            String labelType = labelGrammar.get(3).get(labelGrammar.get(0).indexOf(label));
            double tempConfidenceScore = Double.parseDouble(labelGrammar.get(2).get(labelGrammar.get(0).indexOf(label)));
            double tempMaliciousScore = Double.parseDouble(labelGrammar.get(1).get(labelGrammar.get(0).indexOf(label)));
            switch (labelType) {
                case "PHF":
                    phfScore = (tempConfidenceScore > phfScore) ? tempConfidenceScore : phfScore;
                    break;
                case "CodeSource":
                    codeSourceScore = (tempConfidenceScore > codeSourceScore) ? tempConfidenceScore : codeSourceScore;
                    break;
                case "Feature":
                    featureScore = (tempConfidenceScore > featureScore) ? tempConfidenceScore : featureScore;
                    break;
            }
            maliciousScore = (tempMaliciousScore > maliciousScore) ? tempMaliciousScore : maliciousScore;
        }
        for(int i = 0; i < labelGrammar.get(0).size(); i++){
            if(labelGrammar.get(3).get(i).equals("TRUE")){
                if(!node.labelList.containsKey(labelGrammar.get(0).get(i))){
                    break;
                }
                else{
                    trueScore = trueScore * Double.parseDouble(labelGrammar.get(2).get(i));
                }
            }
        }
        for(String judgeMode : judgeGrammar.keySet()){
            sum = 0;
            int[] judge = new int[judgeGrammar.get(judgeMode).size()];
            for(int i = 0; i < judgeGrammar.get(judgeMode).size(); i++){
                for(String l : judgeGrammar.get(judgeMode).get(i)){
                    if(l.contains("!")){
                        if(!node.labelList.containsKey(l.substring(1))){
                            judge[i] = 1;
                            break;
                        }
                    }
                    else{
                        if(node.labelList.containsKey(l)){
                            judge[i] = 1;
                            break;
                        }
                    }
                }
            }
            for (int aJudge : judge) {
                sum += aJudge;
            }
            confidenceScore = trueScore * (phfScore * 0.45 + codeSourceScore * 0.45 + featureScore * 0.1);
            if(sum == judge.length){
                String comment = outputResult(String.format("txt/%s/result_", clientId) + judgeMode + ".txt", node, maliciousScore, confidenceScore);
                if(node instanceof ProcessNode){
                    /*by yjk
                    Node node1 = node;


                    while(((ProcessNode) node1).parentNode!=null&&((ProcessNode) node1).parentNode.pid!=0)
                    {
                        node1 = ((ProcessNode) node1).parentNode;
                    }


                    if(isOutput(((ProcessNode) node1).pid,judgeMode)){
                    by yjk*/
                        if (judgeMode.contains("online")) {
                            String labelGraphName = outputProcess1((ProcessNode) node, judgeMode);
                            String processGraphName = outputProcessRelation((ProcessNode) node, -1, judgeMode);
                            String processAndLabelGraphName = outputProcessLabelAndRelation((ProcessNode) node, judgeMode);
                            System.out.printf("%d %s %s\n", ((ProcessNode) node).pid, ((ProcessNode) node).processName, judgeMode);
                            String content = ((ProcessNode) node).subjectName + '|'+SupportData.date.getTime()+"|1|1|"+topic+'|'+ CDMDataClassifier.hostid + "||";
                            //SupportData.reporter.report(content);
                            LogUtil logUtil = new LogUtil();
                            LogUtil.insert(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),"spade",""+((ProcessNode) node).pid,node.labelList.keys().toString(),judgeMode,"192.168.10.***");





                        }
                        //reportToDaemon(node, comment, labelGraphName, processGraphName, maliciousScore, confidenceScore, judgeMode);


                }
                node.isOutput = 3;
                break;
            }
        }


//        if(sum == judge.length && node.isOutput <= 3){
//            if(node.isOutput == 3){
//                return;
//            }
//            String comment = outputResult("txt/result1.txt", node, maliciousScore, confidenceScore);
//            if(node instanceof ProcessNode){
//                String labelGraphName = outputProcess1((ProcessNode)node);
//                String processGraphName = outputProcessRelation((ProcessNode)node, -1);
//                reportToDaemon(node, comment, labelGraphName, processGraphName, maliciousScore, confidenceScore, 1);
//            }
//            node.isOutput = 3;
//        }
//        else if((sum == judge.length - 1 || confidenceScore > 0.5) && node.isOutput <= 2){
//            if(node.isOutput == 2){
//                return;
//            }
//            String comment = outputResult("txt/result2.txt", node, maliciousScore, confidenceScore);
//            if(node instanceof ProcessNode){
//                String labelGraphName = outputProcess1((ProcessNode)node);
//                String processGraphName = outputProcessRelation((ProcessNode)node, -1);
//                reportToDaemon(node, comment, labelGraphName, processGraphName, maliciousScore, confidenceScore, 2);
//            }
//            node.isOutput = 2;
//        }
//        else if(sum == judge.length - 1 && node.isOutput == 3){
//            String comment = outputResult("txt/result3.txt", node, maliciousScore, confidenceScore);
//            if(node instanceof ProcessNode){
//                String labelGraphName = outputProcess1((ProcessNode)node);
//                String processGraphName = outputProcessRelation((ProcessNode)node, -1);
//                reportToDaemon(node, comment, labelGraphName, processGraphName, maliciousScore, confidenceScore, 2);
//            }
//            node.isOutput = 2;
//        }
//        else{
//            node.isOutput = 0;
//        }
    }
    private String outputResult(String path, Node node, double maliciousScore, double confidenceScore){
        //output the result of detection into the file
        StringBuilder comment = new StringBuilder();
        try{
            File file = new File(path);
            if (!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write("Time: " + System.currentTimeMillis() + ", ");
            if(node instanceof FileNode){
                FileNode fileNode = (FileNode)node;
                String str = "filepath: " + fileNode.filePath + ", ";
                writer.write(str);
            }
            else{
                ProcessNode processNode = (ProcessNode)node;
                String str = "pid: " + processNode.pid + ", ";
                writer.write(str);
                str ="processname: " + processNode.processName + ", ";
                writer.write(str);
            }
            for(String labelname :node.labelList.keySet()){
                LabelForCS l = node.labelList.get(labelname);
                comment.append(l.labelName).append(".").append(new Date(l.timeStamp / 1000000L).toString()).append(",");
                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                    writer.write("label: " + l.labelName +"   " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) +"   " + l.timeStamp + ", ");
                }
                else{
                    String str = "label: " + l.labelName +"   " + l.timeStamp + ", ";
                    writer.write(str);
                }
            }
            writer.write("maliciousScore: " + maliciousScore + ", ");
            writer.write("confidenceScore: " + confidenceScore + ", ");
            writer.write("\r\n");
            writer.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return comment.toString();
    }
    private void outputLog(Node fatherNode, Node sonNode, int type, LabelForCS label_0, LabelForCS label_1, String grammar){
        //output the log of label transmit into the file
        try{
            File file = new File("txt/log.txt");
            if (!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write("type: " + type + ", ");
            writer.write("father: ");
            if(fatherNode instanceof FileNode){
                FileNode father = (FileNode)fatherNode;
                writer.write("filepath: " + father.filePath + ", ");
            }
            else{
                ProcessNode father = (ProcessNode)fatherNode;
                writer.write("pid: " + father.pid + ", ");
                writer.write("processname: " + father.processName + ", ");
            }
            if(labelGrammar.get(0).indexOf(label_0.labelName) != -1){
                writer.write("label0: " + label_0.labelName + "  " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(label_0.labelName)) + ", ");
            }
            else{
                writer.write("label0: " + label_0.labelName + ", ");
            }

            writer.write("son: ");
            if(sonNode instanceof FileNode){
                FileNode son = (FileNode)sonNode;
                writer.write("filepath: " + son.filePath + ", ");
            }
            else{
                ProcessNode son = (ProcessNode)sonNode;
                writer.write("pid: " + son.pid + ", ");
                writer.write("processname: " + son.processName + ", ");
            }
            if(labelGrammar.get(0).indexOf(label_1.labelName) != -1){
                writer.write("label1: " + label_1.labelName + "  " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(label_1.labelName)) + ", ");
            }
            else{
                writer.write("label1: " + label_1.labelName + ", ");
            }

            if(label_0.timeStamp != -1){
                writer.write("labeltime: " + label_0.timeStamp + ", ");
            }
            else{
                writer.write("labeltime: " + label_1.timeStamp + ", ");
            }
            writer.write("grammar: " + grammar + "\r\n");
            writer.close();
        }
        catch(IOException ignored){
        }
    }
    private void outputFile(ConcurrentHashMap<String, FileNode> fileList, String time)throws IOException{
        //output the log of file list with labels into the file
        File file1 = new File(String.format("csv/%s/", clientId) + time + "_File.csv");
        if (!file1.getParentFile().exists()){
            file1.getParentFile().mkdirs();
        }
        FileWriter writer = new FileWriter(file1, false);
        writer.write("File name,File index,originname,labels\r\n");
        for(String fileName : fileList.keySet()){
            FileNode file = fileList.get(fileName);
            writer.write(fileName);
            writer.write("," + file.index + ",");
            for(String name: file.originFilePath){
                writer.write(name + ";");
            }
            for(String labelname : file.labelList.keySet()){
                LabelForCS l = file.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                    writer.write("," + l.labelName + "   " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + "   " + l.timeStamp);
                }
                else{
                    writer.write("," + l.labelName + "   " + l.timeStamp);
                }
            }
            writer.write("\r\n");
        }
        writer.close();
    }
    private void outputProcessList(ConcurrentHashMap<Integer, ProcessNode> processList, String time)throws IOException{
        //output the log of process list with labels into the file
        File file1 = new File(String.format(String.format("csv/%s/", clientId) + time + "_Process.csv"));
        if (!file1.getParentFile().exists()){
            file1.getParentFile().mkdirs();
        }
        FileWriter writer = new FileWriter(file1, false);
//        writer.write("open process\r\n");
        writer.write("name,pid,ppid,labels\r\n");
        for(int upid : processList.keySet()){
            ProcessNode node = processList.get(upid);
            writer.write(node.processName);
            writer.write("," + node.pid);
            if(node.parentNode != null){
                writer.write("," + node.parentNode.pid);
            }
            else{
                writer.write(",null");
            }
            for(String labelname : node.labelList.keySet()){
                LabelForCS l = node.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                    writer.write("," + l.labelName + "   " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + "   " + l.timeStamp);
                }
                else{
                    writer.write("," + l.labelName + "   " + l.timeStamp);
                }
            }
            writer.write("\r\n");
        }
//        writer.write("close process\r\n");
//        writer.write("name,pid,ppid,labels\r\n");
//        for(long index : closeProcess.keySet()){
//            ProcessNode node = closeProcess.get(index);
//            writer.write(node.processName);
//            writer.write("," + node.pid);
//            if(node.parentNode != null){
//                writer.write("," + node.parentNode.pid);
//            }
//            else{
//                writer.write(",null");
//            }
//            for(String labelname : node.labelList.keySet()){
//                LabelForCS l = node.labelList.get(labelname);
//                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
//                    writer.write("," + l.labelName + "   " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + "   " + l.timeStamp);
//                }
//                else{
//                    writer.write("," + l.labelName + "   " + l.timeStamp);
//                }
//            }
//            writer.write("\r\n");
//        }
        writer.close();
    }
    private void outputProcessList1(ConcurrentHashMap<Integer, ProcessNode> processList, String time)throws IOException{
        //output the log of process list with labels into the file for machine learning
        File file1 = new File(String.format(String.format("csv/%s/", clientId) + time + "_Process1.csv"));
        if (!file1.getParentFile().exists()){
            file1.getParentFile().mkdirs();
        }
        FileWriter writer = new FileWriter(file1, false);
        writer.write("name,pid,feature");
        for(String labelname : labelGrammar.get(0)){
            if(labelname.contains("PT") || labelname.contains("PF")){
                writer.write("," + labelname);
            }
        }
        writer.write("\r\n");
        for(int upid : processList.keySet()){
            ProcessNode node = processList.get(upid);
            writer.write(node.processName + "," + node.pid);
            if(node.isOutput == 3){
                writer.write(",malicious");
            }
            else{
                writer.write(",benign");
            }
            for(String labelname : labelGrammar.get(0)){
                if(labelname.contains("PT") || labelname.contains("PF")){
                    if(node.labelList.containsKey(labelname)){
                        writer.write(",1");
                    }
                    else{
                        writer.write(",0");
                    }
                }
            }
            writer.write("\r\n");
        }
//        for(long index : closeProcess.keySet()){
//            ProcessNode node = closeProcess.get(index);
//            writer.write(node.processName + "," + node.pid);
//            if(node.isOutput == 3){
//                writer.write(",malicious");
//            }
//            else{
//                writer.write(",benign");
//            }
//            for(String labelname : labelGrammar.get(0)){
//                if(labelname.contains("PT") || labelname.contains("PF")){
//                    if(node.labelList.containsKey(labelname)){
//                        writer.write(",1");
//                    }
//                    else{
//                        writer.write(",0");
//                    }
//                }
//            }
//            writer.write("\r\n");
//        }
        writer.close();
    }
    private void outputProcess(ProcessNode processRoot, String time)throws IOException{
        //output the log of process tree with labels into the file
        File file1 = new File(String.format(String.format("dot/%s/", clientId) + time + "_Process.dot"));
        if (!file1.getParentFile().exists()){
            file1.getParentFile().mkdirs();
        }
        FileWriter writer = new FileWriter(file1, false);
        writer.write("digraph graph1 {\r\n");
        writer.write("node [shape = record];\r\n");
        drawProcess2(writer, processRoot);
        writer.write("}\r\n");
        writer.close();
    }

    private void drawProcess2(FileWriter writer, ProcessNode process)throws IOException{
        ArrayList<ProcessNode> PHFNode = findPHFNode(process);
        ArrayList<ProcessNode> plottedNode = new ArrayList<>();
        for(ProcessNode PHFProcess : PHFNode){
            Queue<ProcessNode> nodeToDraw = new LinkedList<>();
            nodeToDraw.add(PHFProcess);
            while (!nodeToDraw.isEmpty()) {
                ProcessNode curr = nodeToDraw.remove();
                if (!plottedNode.contains(curr)) {
                    drawNode(writer, curr, PHFNode);
                    plottedNode.add(curr);
                }
                if (curr.parentNode != null){
                    nodeToDraw.add(curr.parentNode);
                }
                nodeToDraw.addAll(curr.visitProcess.keySet());
            }
        }
        for(ProcessNode node : plottedNode){
            for(ProcessNode subProcess : node.childNode){
                if (plottedNode.contains(subProcess)){
                    writer.write("table" + node.index + " -> table" + subProcess.index + ": head;\r\n");
                }
            }
            for(ProcessNode subProcess : node.visitProcess.keySet()){
                if (plottedNode.contains(subProcess)){
                    for(Operator o : node.visitProcess.get(subProcess)){
                        if(o.operate == 5){
                            writer.write("table" + node.index + " -> table" + subProcess.index + ": head[color = \"red\", label = \"injection\"];\r\n");
                        }
                    }
                }

            }
        }

    }

    private ArrayList<ProcessNode> findPHFNode(ProcessNode process){
        ArrayList<ProcessNode> PHFNode = new ArrayList<>();
        ArrayList<Long> visitedNode = new ArrayList<>();
        Queue<ProcessNode> queue = new LinkedList<>();
        queue.add(process);
        while(!queue.isEmpty()){
            ProcessNode curr = queue.remove();
            for(String labelname : curr.labelList.keySet()) {
                LabelForCS l = curr.labelList.get(labelname);
                if(l!= null && judgePHF(l.labelName) == 1){
                    PHFNode.add(curr);
                }
            }
            visitedNode.add(curr.index);
            queue.addAll(curr.childNode);
        }
        return PHFNode;
    }

    private void drawNode(FileWriter writer, ProcessNode curr, ArrayList<ProcessNode> PHFNode)throws IOException{
        writer.write("table" + curr.index + " [label = \"{<head>processname: " + curr.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + curr.pid + " | cmdline: " + (curr.cmdLine == null ? "" : curr.cmdLine).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + curr.startTime + " | enddtime: " + curr.endTime);
        for (String labelname : curr.labelList.keySet()) {
            LabelForCS l = curr.labelList.get(labelname);
            if (labelGrammar.get(0).indexOf(l.labelName) != -1) {
                writer.write(" | label: " + l.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + " timestamp: " + l.timeStamp);
            } else {
                writer.write(" | label: " + l.labelName + " timestamp: " + l.timeStamp);
            }
        }
        if (PHFNode.contains(curr)){
            writer.write("}\", color = red];\r\n");
        }
        else {
            writer.write("}\"];\r\n");
        }
    }

    private void drawProcess(FileWriter writer, ProcessNode process)throws IOException{
        writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid + " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + process.startTime+ " | enddtime: " + process.endTime);
        int judgePHF = 0;
        for(String labelname : process.labelList.keySet()){
            LabelForCS l = process.labelList.get(labelname);
            if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                writer.write(" | label: " + l.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + " timestamp: " + l.timeStamp);
            }
            else{
                writer.write(" | label: " + l.labelName + " timestamp: " + l.timeStamp);
            }
            if(judgePHF(l.labelName) == 1){
                judgePHF = 1;
            }
        }
        if(judgePHF == 1){
            writer.write("}\", color = red];\r\n");
        }
        else{
            writer.write("}\"];\r\n");
        }
        for(ProcessNode subProcess : process.childNode){
            drawProcess(writer, subProcess);
            writer.write("table" + process.index + " -> table" + subProcess.index + ": head;\r\n");
        }
        for(ProcessNode subProcess : process.visitProcess.keySet()){
            for(Operator o : process.visitProcess.get(subProcess)){
                if(o.operate == 5){
                    writer.write("table" + process.index + " -> table" + subProcess.index + ": head[color = \"red\", label = \"injection\"];\r\n");
                }
            }
        }
    }

    private String chooseProcessRelationToDraw(String pcid, int pid, long startTime, int maxDepth){
        ProcessNode process = TotalStartProcess.get(pcid).get(pid + startTime);
        if (process != null) {
            return outputProcessRelation(process, maxDepth, "query");
        }
        return "";
    }
    public String chooseProcessRelationToDraw (String pcid, int uuid, int maxDepth){
        ProcessNode process = ProcessList.get(pcid).get(uuid);
        if (process != null) {
            return outputProcessRelation(process, maxDepth, "query");
        }
        return "";
    }
    private String outputProcessRelation(ProcessNode process, int maxDepth, String judgeMode){

        String dotFileName = String.format("dot/%s/%s/", clientId,judgeMode) + process.pcid + "_" + process.pid + "_" + process.index + "_ProcessRelation.dot";

        HashSet<String> hasDrawList = new HashSet<>();
        //output the log of process tree with labels into the file for single process
        try {
            File file1 = new File(dotFileName);
            if (!file1.getParentFile().exists()){
                file1.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file1, false);
            writer.write("digraph graph1 {\r\n");
            writer.write("node [shape = record];\r\n");
            writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid + " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + process.startTime+ " | enddtime: " + process.endTime + " | uuid: " + process.upid);
            writer.write("}\", color = red];\r\n");
            hasDrawList.add(process.index + "");
            ProcessNode parentNode = process.parentNode;
            ProcessNode node = process;
            while(parentNode != null && !parentNode.processName.equals("parentNeverShowUp")){
                if (hasDrawList.contains(parentNode.index + "")) {
                    break;
                }
                writer.write("node [shape = record];\r\n");
                writer.write("table" + parentNode.index + " [label = \"{<head>processname: " + parentNode.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + parentNode.pid + " | cmdline: " + parentNode.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + parentNode.startTime+ " | enddtime: " + parentNode.endTime + " | uuid: " + parentNode.upid);
                writer.write("}\"];\r\n");
                writer.write("table" + parentNode.index + " -> table" + node.index + ";\r\n");
                hasDrawList.add(parentNode.index + "");
                node = parentNode;
                parentNode = parentNode.parentNode;
            }
            for(ProcessNode sonNode : process.childNode){
                drawProcessRelation(writer, sonNode, maxDepth, hasDrawList);
                writer.write("table" + process.index + " -> table" + sonNode.index + ";\r\n");
            }
            writer.write("}\r\n");
            writer.close();
        }
        catch (Exception e) {
            System.out.println("Detection Framework: " + e);
        }
        return dotFileName;
    }
    private void drawProcessRelation(FileWriter writer, ProcessNode process, int maxDepth, HashSet<String> hasDrawList)throws IOException{
        if (hasDrawList.contains(process.index + "")) {
            return;
        }
        writer.write("node [shape = record];\r\n");
        writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid + " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") +  " | starttime: " + process.startTime+ " | enddtime: " + process.endTime + " | uuid: " + process.upid);
        writer.write("}\"];\r\n");
        hasDrawList.add(process.index + "");
        if(maxDepth <= 1 && maxDepth != -1){
            return;
        }
        else if(maxDepth > 1){
            maxDepth = maxDepth - 1;
        }
        for(ProcessNode sonNode : process.childNode){
            drawProcessRelation(writer, sonNode, maxDepth, hasDrawList);
            writer.write("table" + process.index + " -> table" + sonNode.index + ";\r\n");
        }
    }

    private String chooseProcessToDraw(String pcid, int pid, long startTime){
        ProcessNode process = TotalStartProcess.get(pcid).get(pid + startTime);
        if (process != null) {
            return outputProcess1(process, "query");
        }
        return "";
    }
    public String chooseProcessToDraw (String pcid, int uuid){
        ProcessNode process = ProcessList.get(pcid).get(uuid);
        if (process != null) {
            return outputProcess1(process, "query");
        }
        return "";
    }
    private String outputProcess1(ProcessNode process, String judgeMode){
        String dotFileName = String.format("dot/%s/%s/", clientId,judgeMode) + process.pcid + "_" + process.pid + "_" + process.index + "_Process.dot";
        //output the log of process tree with labels into the file for single process
        try {
            HashSet<String> hasDrawList = new HashSet<>();
            ArrayList<FileNode> fileDrawList = new ArrayList<>();
            File file1 = new File(dotFileName);
            if (!file1.getParentFile().exists()){
                file1.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file1, false);
            File file2 = new File(String.format("dot/%s/%s/", clientId,judgeMode) + process.pid + "_" + process.index + "_Process.csv");
            if (!file2.getParentFile().exists()){
                file2.getParentFile().mkdirs();
            }
            FileWriter writer1 = new FileWriter(file2, false);
            writer1.write("file name,same file name\r\n");
            writer.write("digraph graph1 {\r\n");
            writer.write("node [shape = Mrecord];\r\n");
            writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid +  " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") +  " | starttime: " + process.startTime+ " | enddtime: " + process.endTime);
            for(String labelname : process.labelList.keySet()){
                LabelForCS l = process.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                    writer.write(" | label: " + l.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + " timestamp: " + l.timeStamp);
                }
                else{
                    writer.write(" | label: " + l.labelName + " timestamp: " + l.timeStamp);
                }
            }
//            int phf = 0, network = 0;
//            for(String labelname : process.labelList.keySet()){
//                if(judgePHF(labelname) == 1 && phf == 0){
//                	phf = 1;
//                    writer.write(" | this process has phf action");
//                }
//                if(labelname.equals("PT1") && network == 0){
//                	network = 1;
//                    writer.write(" | this process has network connection");
//                }
//            }
            writer.write("}\", color = red];\r\n");
            hasDrawList.add(process.index + "");
            for(String labelname : process.labelList.keySet()){
                hasDrawList.add(process.index + labelname);
            }
            for(String labelname : process.labelList.keySet()){
                LabelForCS l = process.labelList.get(labelname);
                for(int i = 0; i < l.sourceLabel.size(); i++){
                    LabelForCS sourceLabel = l.sourceLabel.get(i);
                    String sourcegrammar = l.sourceGrammar.get(i);
                    int judge = drawProcess1(writer, writer1, sourceLabel, sourceLabel.sourceNode, hasDrawList, fileDrawList);
                    if(judge == 1){
                        String []grammar = sourcegrammar.split(",");
                        if(grammar[3].equals("D")){
                            writer.write("table" + sourceLabel.sourceNode.index + " -> table" + process.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                        }
                        else{
                            writer.write("table" + process.index + " -> table" + sourceLabel.sourceNode.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                        }
                    }
                }
            }
            writer.write("}\r\n");
            writer.close();
            writer1.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return dotFileName;
    }
    private int drawProcess1(FileWriter writer, FileWriter writer1, LabelForCS l, Node node, HashSet<String> hasDrawList, ArrayList<FileNode> fileDrawList)throws IOException{
        if(!hasDrawList.contains(node.index + "")){
            hasDrawList.add(node.index + "");
            if(node instanceof ProcessNode){
                ProcessNode process = (ProcessNode)node;
                writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid + " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") +  " | starttime: " + process.startTime+ " | enddtime: " + process.endTime);
            }
            else{
                FileNode file = (FileNode)node;
                for(FileNode filenode : fileDrawList){
                    int judge = 0;
                    for(String labelname : file.labelList.keySet()){
                        if(filenode.labelList.containsKey(labelname)){
                            LabelForCS label_0 = file.labelList.get(labelname);
                            LabelForCS label_1 = filenode.labelList.get(labelname);
                            for(LabelForCS label : label_0.sourceLabel){
                                if(!label_1.sourceLabel.contains(label)){
                                    judge = 1;
                                    break;
                                }
                            }
                        }
                        else{
                            judge = 1;
                        }
                        if(judge == 1){
                            break;
                        }
                    }
                    if(judge == 0){
                        for(String labelname : file.labelList.keySet()){
                            hasDrawList.add(file.index + labelname);
                        }
                        writer1.write(filenode.filePath + "," + file.filePath + "\r\n");
                        return 0;
                    }
                }
                fileDrawList.add(file);
                writer.write("table" + file.index + " [shape = record, label = \"{<head>filename: " + file.filePath.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                if(file.originFilePath.size() == 1){
                    writer.write(" | originName: " + file.originFilePath.get(0).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                }
                else if(file.originFilePath.size() >= 2){
                    writer.write(" | originName: " + file.originFilePath.get(0).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                    writer.write(" | originName: " + file.originFilePath.get(file.originFilePath.size() - 1).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                }
            }
            for(String labelname : node.labelList.keySet()){
                LabelForCS label = node.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(label.labelName) != -1){
                    writer.write(" | label: " + label.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(label.labelName)) + " timestamp: " + label.timeStamp);
                }
                else{
                    writer.write(" | label: " + label.labelName + " timestamp: " + label.timeStamp);
                }
            }
//            int phf = 0, network = 0;
//            for(String labelname : node.labelList.keySet()){
//                if(judgePHF(labelname) == 1 && phf == 0){
//                	phf = 1;
//                    writer.write(" | this process has phf action");
//                }
//                if(labelname.equals("PT1") && network == 0){
//                	network = 1;
//                    writer.write(" | this process has network connection");
//                }
//            }
            writer.write("}\"];\r\n");
        }
        if(!hasDrawList.contains(node.index + l.labelName)){
            hasDrawList.add(node.index + l.labelName);
            for(int i = 0; i < l.sourceLabel.size(); i++){
                LabelForCS sourceLabel = l.sourceLabel.get(i);
                String sourcegrammar = l.sourceGrammar.get(i);
                int judge = drawProcess1(writer, writer1, sourceLabel, sourceLabel.sourceNode, hasDrawList, fileDrawList);
                if(judge == 1){
                    String []grammar = sourcegrammar.split(",");
                    if(grammar[3].equals("D")){
                        writer.write("table" + sourceLabel.sourceNode.index + " -> table" + node.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                    }
                    else{
                        writer.write("table" + node.index + " -> table" + sourceLabel.sourceNode.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                    }
                }
            }
            return 1;
        }
        else{
            return 0;
        }
    }
    private String outputProcessLabelAndRelation(ProcessNode process, String judgeMode){
        String dotFileName = String.format("dot/%s/%s/", clientId,judgeMode) + process.pcid + "_" + process.pid + "_" + process.index + "_LabelAndRelation.dot";
        //output the log of process tree with labels into the file for single process
        try {
            HashSet<String> hasDrawList = new HashSet<>();
            ArrayList<FileNode> fileDrawList = new ArrayList<>();
            HashSet<String> fileRepeatList = new HashSet<>();
            File file1 = new File(dotFileName);
            if (!file1.getParentFile().exists()){
                file1.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(file1, false);
            File file2 = new File(String.format(String.format("dot/%s/%s/", clientId,judgeMode) + process.pid + "_" + process.index + "_LabelAndRelation.csv"));
            if (!file2.getParentFile().exists()){
                file2.getParentFile().mkdirs();
            }
            FileWriter writer1 = new FileWriter(file2, false);
            writer1.write("file name,same file name\r\n");
            writer.write("digraph graph1 {\r\n");
            writer.write("node [shape = Mrecord];\r\n");
            writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid +  " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") +  " | starttime: " + process.startTime+ " | enddtime: " + process.endTime);
            for(String labelname : process.labelList.keySet()){
                LabelForCS l = process.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(l.labelName) != -1){
                    writer.write(" | label: " + l.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(l.labelName)) + " timestamp: " + l.timeStamp);
                }
                else{
                    writer.write(" | label: " + l.labelName + " timestamp: " + l.timeStamp);
                }
            }
            writer.write("}\", color = red];\r\n");
            hasDrawList.add(process.index + "");
            for(String labelname : process.labelList.keySet()){
                hasDrawList.add(process.index + labelname);
            }
            for(String labelname : process.labelList.keySet()){
                LabelForCS l = process.labelList.get(labelname);
                for(int i = 0; i < l.sourceLabel.size(); i++){
                    LabelForCS sourceLabel = l.sourceLabel.get(i);
                    String sourcegrammar = l.sourceGrammar.get(i);
                    drawProcessLabelAndRelation(writer, writer1, sourceLabel, sourceLabel.sourceNode, hasDrawList, fileDrawList, fileRepeatList);
                    if(!fileRepeatList.contains(sourceLabel.sourceNode.index + "")){
                        String []grammar = sourcegrammar.split(",");
                        if(grammar[3].equals("D")){
                            writer.write("table" + sourceLabel.sourceNode.index + " -> table" + process.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                        }
                        else{
                            writer.write("table" + process.index + " -> table" + sourceLabel.sourceNode.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                        }
                    }
                }
            }
            for(ProcessNode visitNode : process.visitProcess.keySet()){
                for(Operator o : process.visitProcess.get(visitNode)){
                    if(o.operate == 5){
                        writer.write("node [shape = record];\r\n");
                        writer.write("table" + visitNode.index + " [label = \"{<head>processname: " + visitNode.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + visitNode.pid + " | cmdline: " + visitNode.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + visitNode.startTime+ " | enddtime: " + visitNode.endTime + " | uuid: " + visitNode.upid);
                        writer.write("}\"];\r\n");
                        writer.write("table" + process.index + " -> table" + visitNode.index + "[label = \"event: inject\"];\r\n");
                        hasDrawList.add(visitNode.index + "");
                        break;
                    }
                }
            }


            ProcessNode parentNode = process.parentNode;
            ProcessNode node = process;
            while(parentNode != null && !parentNode.processName.equals("parentNeverShowUp")){
                if(hasDrawList.contains(parentNode.index + "")) {
                    writer.write("table" + parentNode.index + " -> table" + node.index + "[label = \"event: Create process\"];\r\n");
                    node = parentNode;
                    parentNode = parentNode.parentNode;
                    continue;
                }
                writer.write("node [shape = record];\r\n");
                writer.write("table" + parentNode.index + " [label = \"{<head>processname: " + parentNode.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + parentNode.pid + " | cmdline: " + parentNode.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + parentNode.startTime+ " | enddtime: " + parentNode.endTime + " | uuid: " + parentNode.upid);
                writer.write("}\"];\r\n");
                writer.write("table" + parentNode.index + " -> table" + node.index + "[label = \"event: Create process\"];\r\n");
                hasDrawList.add(parentNode.index + "");
                node = parentNode;
                parentNode = parentNode.parentNode;
            }
            writer.write("}\r\n");
            writer.close();
            writer1.close();
        }
        catch (Exception e) {
            System.out.println("Detection Framework: " + e);
        }
        return dotFileName;
    }
    private int drawProcessLabelAndRelation(FileWriter writer, FileWriter writer1, LabelForCS l, Node node, HashSet<String> hasDrawList, ArrayList<FileNode> fileDrawList, HashSet<String> fileRepeatList)throws IOException{
        if(!hasDrawList.contains(node.index + "")){
            hasDrawList.add(node.index + "");
            if(node instanceof ProcessNode){
                ProcessNode process = (ProcessNode)node;
                writer.write("table" + process.index + " [label = \"{<head>processname: " + process.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + process.pid + " | cmdline: " + process.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") +  " | starttime: " + process.startTime+ " | enddtime: " + process.endTime);
            }
            else{
                FileNode file = (FileNode)node;
                for(FileNode filenode : fileDrawList){
                    int judge = 0;
                    for(String labelname : file.labelList.keySet()){
                        if(filenode.labelList.containsKey(labelname)){
                            LabelForCS label_0 = file.labelList.get(labelname);
                            LabelForCS label_1 = filenode.labelList.get(labelname);
                            for(LabelForCS label : label_0.sourceLabel){
                                if(!label_1.sourceLabel.contains(label)){
                                    judge = 1;
                                    break;
                                }
                            }
                        }
                        else{
                            judge = 1;
                        }
                        if(judge == 1){
                            break;
                        }
                    }
                    if(judge == 0){
                        for(String labelname : file.labelList.keySet()){
                            hasDrawList.add(file.index + labelname);
                        }
                        fileRepeatList.add(file.index +  "");
                        writer1.write(filenode.filePath + "," + file.filePath + "\r\n");
                        return 0;
                    }
                }
                fileDrawList.add(file);
                writer.write("table" + file.index + " [shape = record, label = \"{<head>filename: " + file.filePath.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                if(file.originFilePath.size() == 1){
                    writer.write(" | originName: " + file.originFilePath.get(0).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                }
                else if(file.originFilePath.size() >= 2){
                    writer.write(" | originName: " + file.originFilePath.get(0).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                    writer.write(" | originName: " + file.originFilePath.get(file.originFilePath.size() - 1).replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", ""));
                }
            }
            for(String labelname : node.labelList.keySet()){
                LabelForCS label = node.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(label.labelName) != -1){
                    writer.write(" | label: " + label.labelName + " " + labelGrammar.get(4).get(labelGrammar.get(0).indexOf(label.labelName)) + " timestamp: " + label.timeStamp);
                }
                else{
                    writer.write(" | label: " + label.labelName + " timestamp: " + label.timeStamp);
                }
            }
            writer.write("}\"];\r\n");
            if(node instanceof ProcessNode){
                ProcessNode process = (ProcessNode)node;
                for(ProcessNode visitNode : process.visitProcess.keySet()){
                    for(Operator o : process.visitProcess.get(visitNode)){
                        if(o.operate == 5){
                            writer.write("node [shape = record];\r\n");
                            writer.write("table" + visitNode.index + " [label = \"{<head>processname: " + visitNode.processName.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | pid: " + visitNode.pid + " | cmdline: " + visitNode.cmdLine.replaceAll("\\\\", "\\\\\\\\").replaceAll("[\"{}]", "") + " | starttime: " + visitNode.startTime+ " | enddtime: " + visitNode.endTime + " | uuid: " + visitNode.upid);
                            writer.write("}\"];\r\n");
                            writer.write("table" + process.index + " -> table" + visitNode.index + "[label = \"action: inject\"];\r\n");
                            hasDrawList.add(visitNode.index + "");
                            break;
                        }
                    }
                }
            }
            for(String labelname : node.labelList.keySet()){
                LabelForCS label = node.labelList.get(labelname);
                if(labelGrammar.get(0).indexOf(label.labelName) != -1){
                    if(labelGrammar.get(3).get(labelGrammar.get(0).indexOf(label.labelName)).equals("CodeSource") && !hasDrawList.contains(node.index + label.labelName)){
                        hasDrawList.add(node.index + label.labelName);
                        for(int i = 0; i < label.sourceLabel.size(); i++){
                            LabelForCS sourceLabel = label.sourceLabel.get(i);
                            String sourcegrammar = label.sourceGrammar.get(i);
                            drawProcessLabelAndRelation(writer, writer1, sourceLabel, sourceLabel.sourceNode, hasDrawList, fileDrawList, fileRepeatList);
                            if(!fileRepeatList.contains(sourceLabel.sourceNode.index + "")){
                                String []grammar = sourcegrammar.split(",");
                                if(grammar[3].equals("D")){
                                    writer.write("table" + sourceLabel.sourceNode.index + " -> table" + node.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + label.labelName + "\"];\r\n");
                                }
                                else{
                                    writer.write("table" + node.index + " -> table" + sourceLabel.sourceNode.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + label.labelName + "\"];\r\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        if(!hasDrawList.contains(node.index + l.labelName)){
            hasDrawList.add(node.index + l.labelName);
            for(int i = 0; i < l.sourceLabel.size(); i++){
                LabelForCS sourceLabel = l.sourceLabel.get(i);
                String sourcegrammar = l.sourceGrammar.get(i);
                drawProcessLabelAndRelation(writer, writer1, sourceLabel, sourceLabel.sourceNode, hasDrawList, fileDrawList, fileRepeatList);
                if(!fileRepeatList.contains(sourceLabel.sourceNode.index + "")){
                    String []grammar = sourcegrammar.split(",");
                    if(grammar[3].equals("D")){
                        writer.write("table" + sourceLabel.sourceNode.index + " -> table" + node.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                    }
                    else{
                        writer.write("table" + node.index + " -> table" + sourceLabel.sourceNode.index + "[label = \"action: " + grammar[1] + "  " + sourceLabel.labelName + "->" + l.labelName + "\"];\r\n");
                    }
                }
            }
        }
        return 1;
    }

    private void outputProcessDotAndFileCsv() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
        Date date = new Date();
        String currentTime = format.format(date);

        try {
            for(String pcid : ProcessList.keySet()){

                outputFile(FileList.get(pcid), pcid + "_" + currentTime.replace(':','_'));
                outputProcess(ProcessRoot.get(pcid), pcid + "_" + currentTime.replace(':','_'));
                outputProcessList(ProcessList.get(pcid), pcid + "_" + currentTime.replace(':','_'));
                outputProcessList1(ProcessList.get(pcid), pcid + "_" + currentTime.replace(':','_'));
            }

        }
        catch (Exception e) {
            System.out.println("Detection Framework: " + e);
        }

    }
/*
    private void reportToDaemon(Node node, String comment, String labelGraphName, String processGraphName, double maliciousScore, double confidenceScore, String alertMode) {
        int alertLevel = judgeMode2AlertLevel.get(alertMode);
        ProcessNode processNode = (ProcessNode)node;
        int uuid = processNode.upid;
        ProcessTable processTable = ProcessTable.getInstance(clientId);
        Process process = processTable.getProcessByUuid(uuid);


        try (BufferedWriter bw = new BufferedWriter(new FileWriter("getByUUID", true))) {
            bw.write(String.format("pid: %d, uuid: %d, cmd: %s, pname: %s, %s\n", ((ProcessNode) node).pid, ((ProcessNode) node).upid, ((ProcessNode) node).cmdLine, ((ProcessNode) node).processName, process));
        } catch (Exception e) {
            e.printStackTrace();
        }


        SocketReporter reporter = new SocketReporter();
        reporter.report(clientId, process, comment, maliciousScore, confidenceScore, alertLevel, labelGraphName, processGraphName);
    }
 */
}
