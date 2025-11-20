public class Process {

    private long PID;
    private long arrivalTime;
    private long burstTime;
    private int priority;
    private long memoryReq;
    private int devReq;
    private int state;
    private long completionTime;
    private long turnaroundTime;
    private long waitingTime;

    private long remainingBurstTime;

    // valid states
    // 1 -> HoldQueue1
    // 2 -> HoldQueue2
    // 3 -> readyQueue
    // 4 -> Running
    // 5 -> finished
    // -1 -> rejected

    public Process(long PID, long arrivalTime, long burstTime, int priority, long memoryReq, int devReq){
        this.PID = PID;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.memoryReq = memoryReq;
        this.devReq = devReq;
        this.remainingBurstTime = this.burstTime;
    }

    public long getMemoryReq(){
        return this.memoryReq;
    }
    public int getDevReq(){
        return this.devReq;
    }
    public int getPriority(){return this.priority;}
    public long getRemainingBurstTime(){
        return this.remainingBurstTime;
    }
    public long getPID(){return this.PID;}

    public void setState(int state){this.state = state;}
    public void setRemainingBurstTime(long remainingBurstTime){this.remainingBurstTime = remainingBurstTime;}
    public int getState(){return this.state;}

    public void setCompletion(long t) { this.completionTime = t; }
    public long getCompletion() { return this.completionTime; }
    public long getTurnaround() { return this.turnaroundTime; }
    public long getWaiting() { return this.waitingTime; }
    public void computeTimes() {
        this.turnaroundTime = completionTime - arrivalTime;
        this.waitingTime = turnaroundTime - burstTime;
    }
    public long getArrivalTime(){return this.arrivalTime;}
}
