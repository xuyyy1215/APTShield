package MarpleNu.CDMDataDump;

import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AllDataDump extends DataDump {
    private String path = "./dump.json";
    private FileWriter fileWriter;
    private void init() throws IOException {
        File file = new File(path);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        fileWriter = new FileWriter(file, true);
    }

    public AllDataDump() throws IOException {
        init();
    }

    AllDataDump(String path) throws IOException {
        this.path = path;
        init();
    }

    @Override
    public void dump(TCCDMDatum tccdmDatum) throws IOException {
        fileWriter.write(tccdmDatum.toString()+'\n');
    }
}
