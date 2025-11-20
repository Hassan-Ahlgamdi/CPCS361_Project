import java.util.Collections;
import java.util.LinkedList;

public class Queue {
    private LinkedList<Process> processList = new LinkedList<Process>();
    private String schType;

    public Queue(String schType){
        this.schType = schType;
    }

    public void enqueue(Process p){
        // add the process to the queue
        this.processList.add(p);
        // sort the queue if its HQ1
        if (this.schType.equalsIgnoreCase("MinMemory")){
            this.processList.sort((p1,p2) -> Long.compare(p1.getMemoryReq(), p2.getMemoryReq()));
        }
    }

    public Process dequeue(){
        return this.processList.poll();
    }

    public boolean isEmpty(){
        return this.processList.isEmpty();
    }

    public int size(){
        return this.processList.size();
    }

    public Process peek(){
        return this.processList.peek();
    }

    public LinkedList<Process> getProcessList(){
        return this.processList;
    }

    public Process[] toList() {
        return this.processList.toArray(new Process[this.processList.size()]);
    }

    // Sorts the queue elements by PID (Ascending)
    public void sortById() {
        this.processList.sort((p1, p2) -> Long.compare(p1.getPID(), p2.getPID()));
    }
}
