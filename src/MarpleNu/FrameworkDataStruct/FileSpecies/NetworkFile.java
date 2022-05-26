package MarpleNu.FrameworkDataStruct.FileSpecies;

public class NetworkFile {
    CharSequence remoteIP;
    int remotePort;
    CharSequence localIP;
    int localPort;
    int ipProtocol;
    public NetworkFile(CharSequence remoteIP, int remotePort, CharSequence localIP, int localPort, int ipProtocol)
    {
        this.remoteIP=remoteIP;
        this.remotePort=remotePort;
        this.localIP=localIP;
        this.localPort=localPort;
        this.ipProtocol=ipProtocol;
    }

}
