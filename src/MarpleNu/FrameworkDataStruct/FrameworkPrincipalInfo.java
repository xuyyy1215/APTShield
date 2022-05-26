package MarpleNu.FrameworkDataStruct;

import com.bbn.tc.schema.avro.cdm19.*;


import java.util.List;

public class FrameworkPrincipalInfo {
    private final UUID uuid;
    private final CharSequence userId;
    private final CharSequence username;
    private final List<CharSequence> groupIds;
    private final PrincipalType type;
    public FrameworkPrincipalInfo(Principal principal)
    {
        this.uuid=principal.getUuid();
        this.userId=principal.getUserId();
        this.username=principal.getUsername();
        this.groupIds=principal.getGroupIds();
        this.type=principal.getType();
    }

}
