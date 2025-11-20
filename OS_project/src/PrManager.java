import java.util.Locale;

public class PrManager {

    // States (use constants instead of magic ints)
    public static final int STATE_NEW = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_HOLD = 3;
    public static final int STATE_FINISHED = 4;
    public static final int STATE_REJECTED = 5;

    private long internalClock;

    private final Queue holdQueue1;
    private final Queue holdQueue2;
    private final Queue readyQueue;
    private final Queue finishedQueue = new Queue("Finish");
    private final OtherKerServices kernel;
    private Scheduler scheduler;

    private Process currentProcess;
    private long sliceStartTime = 0;
    private long sliceEndTime = Long.MAX_VALUE;

    public PrManager(OtherKerServices kernel) {
        this.holdQueue1 = new Queue("MinMemory");
        this.holdQueue2 = new Queue("FIFO");
        this.readyQueue = new Queue("FIFO");
        this.kernel = kernel;
        this.internalClock = 0;
        this.scheduler = null;
    }
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        if(this.scheduler != null) this.scheduler.updateQuantum(this.readyQueue);
    }
    public long dispatch(long currentTime){

            // If CPU is idle
            if (this.currentProcess == null) {

                // If nothing to run
                if (this.readyQueue.isEmpty()) {
                    this.sliceEndTime = Long.MAX_VALUE;
                    return this.sliceEndTime;
                }

                // Update quantum
                if (this.scheduler != null) {
                    this.scheduler.updateQuantum(this.readyQueue);
                }

                // Select the next process
                Process next = this.scheduler.selectNextProcess(this.readyQueue);
                if (next == null) {
                    this.sliceEndTime = Long.MAX_VALUE;
                    return this.sliceEndTime;
                }

                // Start running the process
                this.currentProcess = next;
                this.currentProcess.setState(STATE_RUNNING);
                this.sliceStartTime = currentTime;

                long quantum = (this.scheduler != null) ? this.scheduler.getTimeQuantum() : Long.MAX_VALUE;
                if (quantum <= 0) quantum = this.currentProcess.getRemainingBurstTime();

                long sliceLength = Math.min(this.currentProcess.getRemainingBurstTime(), quantum);
                this.sliceEndTime = currentTime + sliceLength;
                return this.sliceEndTime;
            }

            // CPU already running → return current slice end time
            return this.sliceEndTime;
        }

    public void procArrivingRoutine(long PID, long arrivalTime, long burstTime, int priority, long memoryReq, int devReq) {
        Process p = new Process(PID, arrivalTime, burstTime, priority, memoryReq, devReq);

        // Reject job if it exceeds system limits
        if (memoryReq > kernel.getSystemMemorySize() || devReq > kernel.getSystemNoDevs()) {
            System.out.println("The process with PID: " + PID + " is greater than the system config");
            p.setState(STATE_REJECTED);
            return;
        }

        // If job CAN RUN NOW (memory & devices free AND CPU idle),
        // → allocate resources → move to READY queue.
        if (memoryReq <= kernel.getCurrentMemorySize() &&
                devReq <= kernel.getCurrentNoDevs())
        {
            kernel.allocateMemory(p);
            kernel.reserveDevices(p);
            p.setState(STATE_READY);
            readyQueue.enqueue(p);

            // DRR quantum must update because ready queue changed
            if (scheduler != null) scheduler.updateQuantum(readyQueue);
            return;
        }

        // Otherwise → put job into Hold queue (HQ1 or HQ2)
        p.setState(STATE_HOLD);

        if (priority == 1)
            holdQueue1.enqueue(p);
        else
            holdQueue2.enqueue(p);
    }

    public void cpuTimeAdvance(long duration){
        this.internalClock += duration;
    }

    private void tryAdmitFromHolds() {
        // HQ1 first, but only one job admitted at a time
        if (!holdQueue1.isEmpty()) {
            Process h = holdQueue1.peek();
            if (h.getMemoryReq() <= kernel.getCurrentMemorySize() &&
                    h.getDevReq() <= kernel.getCurrentNoDevs()) {

                holdQueue1.dequeue();
                kernel.allocateMemory(h);
                kernel.reserveDevices(h);
                h.setState(STATE_READY);
                readyQueue.enqueue(h);
                if (scheduler != null) scheduler.updateQuantum(readyQueue);
                return; // <-- IMPORTANT: only admit ONE job
            }
        }

        // HQ2 next (one job only)
        if (!holdQueue2.isEmpty()) {
            Process h = holdQueue2.peek();
            if (h.getMemoryReq() <= kernel.getCurrentMemorySize() &&
                    h.getDevReq() <= kernel.getCurrentNoDevs()) {

                holdQueue2.dequeue();
                kernel.allocateMemory(h);
                kernel.reserveDevices(h);
                h.setState(STATE_READY);
                readyQueue.enqueue(h);
                if (scheduler != null) scheduler.updateQuantum(readyQueue);
            }
        }
    }

    public void handleInternalEvent(long currentTime) {
        if (this.currentProcess == null) return;

        long executed = currentTime - this.sliceStartTime;
        long newRemaining = this.currentProcess.getRemainingBurstTime() - executed;
        this.currentProcess.setRemainingBurstTime(Math.max(0, newRemaining));

        // PROCESS FINISHED
        if (this.currentProcess.getRemainingBurstTime() == 0) {

            this.currentProcess.setState(STATE_FINISHED);
            this.currentProcess.setCompletion(currentTime);
            this.currentProcess.computeTimes();

            // free resources
            kernel.deallocateMemory(this.currentProcess);
            kernel.releaseDevices(this.currentProcess);

            // move to finished queue
            finishedQueue.enqueue(this.currentProcess);

            // CPU now empty
            this.currentProcess = null;
            this.sliceStartTime = 0;
            this.sliceEndTime = Long.MAX_VALUE;

            // ✔ Admit ONLY ONE job now (correct OS logic)
            tryAdmitFromHolds();

            // ✔ Update DRR quantum because ready queue changed
            if (scheduler != null) scheduler.updateQuantum(readyQueue);

            // ✔ Begin next slice
            dispatch(currentTime);
            return;
        }

        // QUANTUM EXPIRED (preempt)
        this.currentProcess.setState(STATE_READY);
        readyQueue.enqueue(this.currentProcess);

        this.currentProcess = null;
        this.sliceStartTime = 0;
        this.sliceEndTime = Long.MAX_VALUE;

        // Update quantum for DRR
        if (scheduler != null) scheduler.updateQuantum(readyQueue);

        // Dispatch next process
        dispatch(currentTime);
    }

    public void admitOneFromHolds() {
        tryAdmitFromHolds(); // reuse your existing private helper
    }

    // getters for display / stats
    public Queue getReadyQueue() { return this.readyQueue; }
    public Queue getHoldQueue1() { return this.holdQueue1; }
    public Queue getHoldQueue2() { return this.holdQueue2; }
    public Queue getFinishedQueue() { return this.finishedQueue; }
    public Process getCurrentProcess() { return this.currentProcess; }
    public long getSliceEndTime() { return this.sliceEndTime; }
    public long getInternalClock() { return this.internalClock; }
    public OtherKerServices getKernel() { return this.kernel; }

    public long getNextDecisionTime(){
        return 1;
    }

    public long getRunningProcId(){
        return this.currentProcess.getPID();
    }

    public String printReadyList() {
        StringBuilder sb = new StringBuilder("");
        for (Process p : this.readyQueue.toList()) {
            sb.append(String.format("   %d\n", p.getPID()));
        }
        sb.append("\n");
        return sb.toString();
    }
    public String printHold1() {
        StringBuilder sb = new StringBuilder();
        for (Process p : this.holdQueue1.toList()) {
            sb.append(String.format(Locale.US, "  %d\n", p.getPID()));
        }
        sb.append("\n");
        return sb.toString();
    }

    public String printHold2() {
        StringBuilder sb = new StringBuilder();
        for (Process p : this.holdQueue2.toList()) {
            sb.append(String.format(Locale.US, "  %d\n", p.getPID()));
        }
        sb.append("\n");
        return sb.toString();
    }



    public String printFinishedJobs() {
        StringBuilder sb = new StringBuilder();
        // Sort by PID to match professor's output
        this.finishedQueue.sortById();

        for (Process p : this.finishedQueue.toList()) {
            sb.append(String.format(Locale.US, "  %-6d %-15.2f %-16.2f %-17.2f %.2f\n",
                    p.getPID(),
                    (double) p.getArrivalTime(),
                    (double) p.getCompletion(),
                    (double) p.getTurnaround(),
                    (double) p.getWaiting()));
        }
        return sb.toString();
    }
}
