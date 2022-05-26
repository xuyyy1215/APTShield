package MarpleNu.CDMDataParser;


import MarpleNu.FrameworkDataStruct.FrameworkPrincipalInfo;
import MarpleNu.FrameworkSupportData.SupportData;
import com.bbn.tc.schema.avro.cdm19.*;


public class PrincipalParser extends BaseParser{
    private final SupportData supportData;

    public PrincipalParser(SupportData supportData)
    {
        this.supportData = supportData;
    }

    @Override
    public void parse(TCCDMDatum tccdmDatum)
    {
        Principal principal = (Principal)tccdmDatum.getDatum();
        FrameworkPrincipalInfo principalInfo =
                new FrameworkPrincipalInfo(principal);
        supportData.uuid2Principal.put(principal.getUuid(),principalInfo);
    }
}
