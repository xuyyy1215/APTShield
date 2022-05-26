package MarpleNu.LinuxFrameworkConfig;

import java.io.*;
import java.util.HashMap;

public class FrameworkConfig {
    private static final String default_path = "./config/basic.conf";
    private static HashMap<String,String> GobalConfigMap = new HashMap<>();

    public static void init() throws IOException {
        init(default_path);
    }


    private static void init(String filename) throws IOException {
        GobalConfigMap = buildmap(filename);
    }

    public static HashMap<String, String> buildmap(String filename) throws IOException{
        HashMap<String,String> tmpMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            String[] tmp = line.split("=");
            if (tmp.length >= 2){
                String key = tmp[0].trim();
                String value = tmp[1].trim();
                tmpMap.put(key,value);
            }
        }
        return tmpMap;
    }

    public static String Search(String key) {
        if (key == null)
            return null;
        return GobalConfigMap.get(key);
    }
}
