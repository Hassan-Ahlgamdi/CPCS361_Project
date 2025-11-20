import java.util.LinkedList;

public class DRoundRobinScheduler extends Scheduler {

    private static long SR;
    private static long AR;
    private int timeQunt;

    public DRoundRobinScheduler() {
        super();
        this.SR = 0;
        this.AR = 0;
        this.timeQunt = 0;
    }
    @Override
    protected Process selectNextProcess(Queue readyQueue) {
        if (readyQueue.isEmpty())
            return null;
        updateQuantum(readyQueue);
        return readyQueue.dequeue();
    }
    @Override
    protected int getTimeQuantum(){
        return (int) this.AR;
    }

    public long getSR(){
        return this.SR;
    }
    public long getAR(){
        return this.AR;
    }

    @Override
    public void updateQuantum(Queue readyQueue) {
        if (readyQueue == null || readyQueue.isEmpty()) {
            this.SR = 0;
            this.AR = 0;
            return;
        }

        // compute SR using remaining burst times
        long sum = 0;
        LinkedList<Process> list = readyQueue.getProcessList();
        for (Process p : list) {
            sum += p.getRemainingBurstTime();
        }
        this.SR = sum;
        int n = readyQueue.size();
        if (n > 0) {
            // use ceiling to avoid quantum 0 due to integer truncation
            this.AR = (long) Math.round((double) this.SR / (double) n);
        } else {
            this.AR = 0;
        }
    }
}
