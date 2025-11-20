public class OtherKerServices {
    private final long systemMemorySize;
    private final int systemNoDevs;
    private long currentMemorySize;
    private int currentNoDevs;

    // getters and setters
    public OtherKerServices(long memorySize, int noDevs){
        this.systemMemorySize =  this.currentMemorySize = memorySize;
        this.systemNoDevs = this.currentNoDevs = noDevs;
    }
    public long getCurrentMemorySize() {return this.currentMemorySize;}
    public int getCurrentNoDevs() {return this.currentNoDevs;}
    public long getSystemMemorySize() {return this.systemMemorySize;}
    public int getSystemNoDevs() {return this.systemNoDevs;}


    public void allocateMemory(Process p){this.currentMemorySize -= p.getMemoryReq();}
    public void deallocateMemory(Process p){this.currentMemorySize += p.getMemoryReq();}
    public void reserveDevices(Process p){this.currentNoDevs -= p.getDevReq();}
    public void releaseDevices(Process p){this.currentNoDevs += p.getDevReq();}
}
