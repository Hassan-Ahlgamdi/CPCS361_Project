public abstract class Scheduler {

    protected abstract Process selectNextProcess(Queue readyQueue);
    protected abstract int getTimeQuantum();
    public abstract void updateQuantum(Queue readyQueue);
}
