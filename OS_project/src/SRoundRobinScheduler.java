public class SRoundRobinScheduler extends Scheduler{

    private final int timeQunt = 14;

    @Override
    protected Process selectNextProcess(Queue readyQueue) {
        if(!readyQueue.isEmpty())
            return readyQueue.dequeue();
        return null;
    }
    @Override
    protected int getTimeQuantum(){
        return this.timeQunt;
    }
    @Override
    public void updateQuantum(Queue readyQueue) {
        // nothing changes here
    }
}
