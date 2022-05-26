package MarpleNu.FrameworkDataStruct;

import com.bbn.tc.schema.avro.cdm19.*;

import java.util.List;

public class FrameworkProcessInfo {
    private final int pid;
    private int ppid;
    private int tgid;
    private final UUID uuid;
    private final String path;
    private final String name;
    private String cmdline;
    private boolean isNetworkConnect;
    private PrivilegeLevel privilegeLevel;
    private List<CharSequence> importedLibraries;


    public FrameworkProcessInfo(UUID uuid, int pid, int ppid, String path, String name)
    {
        this.uuid = uuid;
        this.path = path;
        this.pid = pid;
        this.ppid = ppid;
        this.name = name==null?"":name;
        this.cmdline = "";
        this.tgid = pid;
    }

    public int getPid() {
        return pid;
    }

    public int getPpid() {
        return ppid;
    }

    public int getTgid() {
        return tgid;
    }

    public void setTgid(int val) {
        tgid = val;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isNetworkConnect() {
        return isNetworkConnect;
    }

    public void setNetworkConnect(boolean networkConnect) {
        isNetworkConnect = networkConnect;
    }

    public String getCmdline() {
        return cmdline;
    }

    public void setCmdline(String cmdline) {
        this.cmdline = cmdline;
    }

    public PrivilegeLevel getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(PrivilegeLevel pLevel) {
        privilegeLevel = pLevel;
    }

    public List<CharSequence> getImportedLibraries() {
        return importedLibraries;
    }

    public void setImportedLibraries(List<CharSequence> libraries) {
        importedLibraries = libraries;
    }

    public void setPpid(int ppid) {
        this.ppid = ppid;
    }
}
