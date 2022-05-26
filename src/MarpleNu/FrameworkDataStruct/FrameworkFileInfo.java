package MarpleNu.FrameworkDataStruct;


import com.bbn.tc.schema.avro.cdm19.UUID;


public class FrameworkFileInfo {
    public  static final int srcSink = 0x00;
    private static final int dir = 0x01;
    public static final int file = 0x02;
    public static final int socket = 0x03;
    private static final int pipe = 0x04;
    public static final int ipc = 0x05;

    public int getProperty() {
        return property;
    }

    private int property;
    private static final int FlagDownlaodFile= 0x0001;
    private static final int FlagSenstiveFile= 0x0002;
    private static final int FlagNetworkFile= 0x0004;
    private static final int FlagSocket= 0x0008;
    private static final int FlagFile= 0x0010;
    private static final int FlagDir=0x0020;
    private static final int FlagRCScript=0x0040;
    private static final int FlagTimeBaseExecution=0x0080;
    private static final int FlagFileInfectionAndReplacement=0x0100;
    private static final int FlagTop10SEtcFileSystemAccesses=0x0200;
    private static final int FlagTop10SSysFileSystemAccesses=0x0400;
    private static final int FlagTop10ProcFileSystemAccesses=0x0800;
    private static final int FlagDeception=0x1000;
    private static final int FlagLog=0x2000;
    private static final int FlagSignal=0x4000;
    private static final int FlagEvent=0x8000;
    private int flag;

    private static final String[] Top10ProcFileSystemAccesses = {
            "/proc/net/route",
            "/proc/filesystems",
            "/proc/stat",
            "/proc/net/tcp",
            "/proc/meminfo",
            "/proc/net/dev",
            "/proc/cmdline",
            "/proc/cpuinfo"
    };

    private static final String[] Top10SysFileSystemAccesses = {
            "/sys/devices/system/cpu/online",
            "/sys/devices/system/node/node0/meminfo",
            "/sys/module/x_tables/inistate",
            "/sys/module/ip_tables/inistate",
            "/sys/class/dmi/id/sys_vendor",
            "/sys/class/dmi/id/product_name",
            "/sys/firmware/efi/systable",
    };

    private static final String[] Top10SEtcFileSystemAccesses = {
            "/etc/rc.d/rc.local",
            "/etc/rc.conf",
            "/etc/resolv.cof",
            "/etc/nsswitch.conf",
            "/etc/hosts",
            "/etc/rc.local",
            "/etc/localtime",
            "/etc/cron.deny"
    };

    /*by yjk*/
    private static final String[] UploadDirectories = {
            "/phpstudy/www/DVWA/vulnerabilities/upload/hackable/uploads",
            "/phpstudy/www/DVWA/hackable/uploads",
            "/var/www/html/web3/WordPress-4.6.18/wp-content/uploads",
            "/var/lib/tomcat8/webapps/jsp/",
            "/usr/tomcat7/webapps/upload/upload/",
            "/var/www/html/uploads",
            "/www/admin/localhost_80/wwwroot/Eyoucms/",
            "/www/admin/localhost_80/wwwroot/Dedecms/"
    };
    /*by yjk*/


    private void init()
    {
        flag=0;
    }

    public FrameworkFileInfo(UUID uuid, String fileName, int property)
    {
        init();
        this.property = property;
        setFileFlag(fileName);
    }

    public FrameworkFileInfo(UUID uuid,  int property)
    {
        init();
        this.property = property;
    }

    public static boolean checkFileSensitive(String fileName)
    {
        //fileName.startsWith("/proc/cmdline")
        //    || fileName.startsWith("/etc/system.d")
        //    || fileName.startsWith("/etc/rc")
        //   || fileName.startsWith("/etc/init")
        /*||*/
        //                || fileName.contains(".bash_profile")
        //                || fileName.contains(".bash_login")
        //                || fileName.contains(".bashrc")
        //                || fileName.contains("bash.bashrc")
        // || fileName.contains("profile.d")
        return fileName.startsWith("/etc/shadow")
                //   || fileName.startsWith("/etc/group")
                //   || fileName.startsWith("/etc/gshadow")
                //   || fileName.startsWith("/etc/pam.d")
                || fileName.startsWith("/etc/passwd")
                || fileName.startsWith("/etc/sudoers");
    }

    /*by yjk*/
    public static boolean isUploadDirectory(String fileName)
    {
        for(String dir : UploadDirectories){
            if(fileName.startsWith(dir)){
                return true;
            }
        }
        return false;
    }
    /*by yjk*/

    private void setFileFlag(String fileName)
    {
        if (checkFileSensitive(fileName))
            flag |= FlagSenstiveFile;

        if (fileName.startsWith("/var/spool/sron"))
            flag |= FlagRCScript;

        if (fileName.startsWith("/bin/") || fileName.startsWith("/usr/bin/"))
            flag |= FlagFileInfectionAndReplacement;

        if (fileName.startsWith("/proc/"))
            flag |= FlagDeception;

        if (fileName.startsWith("/var/log") || fileName.startsWith("/var/log/wtmp"))
            flag |= FlagLog;

        for (String i:Top10ProcFileSystemAccesses){
            if (fileName.contains(i))
            {
                flag |= FlagTop10ProcFileSystemAccesses;
                break;
            }
        }

        for (String i:Top10SEtcFileSystemAccesses){
            if (fileName.contains(i))
            {
                flag |= FlagTop10SEtcFileSystemAccesses;
                break;
            }
        }

        for (String i:Top10SysFileSystemAccesses){
            if (fileName.contains(i))
            {
                flag |= FlagTop10SSysFileSystemAccesses;
                break;
            }
        }

    }

    public void merge(FrameworkFileInfo from)
    {
        flag |= from.flag;
    }

    public Boolean getDownlaodFile() {
        return (flag & FlagDownlaodFile)!=0;
    }

    public Boolean getSenstiveFile() {
        return (flag & FlagSenstiveFile)!=0;
    }

    public Boolean getIsNetworkFile(){
        return (flag & FlagNetworkFile)!=0;
    }

    public Boolean getSocket() {
        return (flag & FlagSocket)!=0;
    }

    public Boolean getFile() {
        return (flag & FlagFile)!=0;
    }

    public Boolean getDir() {
        return (flag & FlagDir)!=0;
    }

    public Boolean getRCScript() {
        return (flag & FlagRCScript)!=0;
    }

    public Boolean getTimeBaseExecution() {
        return (flag & FlagTimeBaseExecution)!=0;
    }

    public Boolean getFileInfectionAndReplacement() {
        return (flag & FlagFileInfectionAndReplacement)!=0;
    }

    public Boolean getTop10SEtcFileSystemAccesses() {
        return (flag & FlagTop10SEtcFileSystemAccesses)!=0;
    }

    public Boolean getTop10SSysFileSystemAccesses() {
        return (flag & FlagTop10SSysFileSystemAccesses)!=0;
    }

    public Boolean getTop10ProcFileSystemAccesses() {
        return (flag & FlagTop10ProcFileSystemAccesses)!=0;
    }

    public Boolean getDeception() {
        return (flag & FlagDeception)!=0;
    }

    public Boolean getLog() {
        return (flag & FlagLog)!=0;
    }

    public Boolean getSignal() {
        return (flag & FlagSignal)!=0;
    }

    public Boolean getEvent() {
        return (flag & FlagEvent)!=0;
    }

    public void setProperty(int property) {
        this.property = property;
    }


    public void setDownlaodFile(Boolean downlaodFile) {
        flag |= (downlaodFile?FlagDownlaodFile:0);
    }

    public void setSocket(Boolean socket) {
        flag |= (socket?FlagSocket:0);
    }

    public void setFile(Boolean file) {
        flag |= (file?FlagFile:0);
    }

    public void setDeception(Boolean deception) {
        flag |= (deception?FlagDeception:0);
    }

    public void setLog(Boolean log) {

        flag |= (log?FlagLog:0);
    }

    public void setSignal(Boolean signal) {
        flag |= (signal?FlagSignal:0);
    }

    public void setEvent(Boolean event) {
        flag |= (event?FlagEvent:0);
    }


}
