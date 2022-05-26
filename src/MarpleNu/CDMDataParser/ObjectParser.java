package MarpleNu.CDMDataParser;

import MarpleNu.FrameworkDataStruct.FrameworkFileInfo;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.FrameworkTools.CDMDataTools;
import com.bbn.tc.schema.avro.cdm19.*;


public class ObjectParser extends BaseParser{
    private final SupportData supportData;

    public ObjectParser(SupportData supportData)
    {
        this.supportData = supportData;
    }

    @Override
    public void parse(TCCDMDatum tccdmDatum)
    {
        String objectName=tccdmDatum.getDatum().getClass().getName();
        if(objectName.endsWith("FileObject")) {
            FileObject record = (FileObject) tccdmDatum.getDatum();
            String path = CDMDataTools.getPropertiesValue(record.getBaseObject(),"path");
            if (path.startsWith("/dev/pts")||path.startsWith("/dev/tty")) {
            }
            /*
            FrameworkFileInfo frameworkFileInfo = new FrameworkFileInfo(
                    record.getUuid(),
                    path,
                    FrameworkFileInfo.file
            );
            */
            //supportData.uuid2FileMap.put(record.getUuid(), frameworkFileInfo);


        }
        else if(objectName.endsWith("NetFlowObject")){
            NetFlowObject record=(NetFlowObject)tccdmDatum.getDatum();
            FrameworkFileInfo frameworkFileInfo = new FrameworkFileInfo(
                    record.getUuid(),
                    FrameworkFileInfo.socket
            );
            supportData.uuid2FileMap.put(record.getUuid(), frameworkFileInfo);
        }
        else if(objectName.endsWith("SrcSinkObject")){
            /*
            SrcSinkObject record = (com.bbn.tc.schema.avro.cdm19.SrcSinkObject)tccdmDatum.getDatum();
            FrameworkFileInfo frameworkFileInfo = new FrameworkFileInfo(
                    record.getUuid(),
                    record.getFileDescriptor(),
                    FrameworkFileInfo.srcSink
            );
            supportData.uuid2FileMap.put(record.getUuid(), frameworkFileInfo);
            */
        }
        else if(objectName.endsWith("IpcObject")){
            IpcObject record = (IpcObject)tccdmDatum.getDatum();
            String pid=null;

            for (CharSequence key : record.getBaseObject().getProperties().keySet()) {
                if (key.toString().equals("pid"))
                    pid = record.getBaseObject().getProperties().get(key).toString();
            }
            FrameworkFileInfo frameworkFileInfo = new FrameworkFileInfo(
                    record.getUuid(),
                    FrameworkFileInfo.ipc
            );
            supportData.uuid2FileMap.put(record.getUuid(), frameworkFileInfo);
        }
    }

}
