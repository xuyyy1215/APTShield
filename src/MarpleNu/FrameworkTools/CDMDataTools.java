package MarpleNu.FrameworkTools;

import com.bbn.tc.schema.avro.cdm19.AbstractObject;

public class CDMDataTools {
    public static String getPropertiesValue(AbstractObject ob, String key)
    {
        String ret = "";
        for (CharSequence cs : ob.getProperties().keySet()) {
            if (cs.toString().equals("path"))
                ret = ob.getProperties().get(cs).toString();
        }
        return ret;
    }
}
