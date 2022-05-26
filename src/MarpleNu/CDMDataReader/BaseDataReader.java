package MarpleNu.CDMDataReader;

import com.bbn.tc.schema.avro.cdm19.TCCDMDatum;

import java.util.ArrayDeque;
import java.util.Deque;

public class BaseDataReader implements Runnable{
    Deque<TCCDMDatum> tccdmDatumList= new ArrayDeque<>();
    BaseDataReader()
    {
    }

    public TCCDMDatum getData()
    {
        return tccdmDatumList.pollFirst();
    }

    public int getSize()
    {
        return tccdmDatumList.size();
    }

    @Override
    public void run()
    {

    }


}
