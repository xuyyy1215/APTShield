package MarpleNu.FrameworkDataStruct.FileSpecies;

public class IPCFile {
    private final int fd1;
    private final int fd2;
    private final int pid;
    public IPCFile(int fd1, int fd2, int pid)
    {
        this.fd1 = fd1;
        this.fd2 = fd2;
        this.pid = pid;
    }
}
