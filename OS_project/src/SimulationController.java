import java.io.*;
import java.util.Scanner;
import java.util.Locale;
class SimulationController {

    private static long currentTime;
    private static File inputFile = new File("IO\\inputSRR.txt");
    private static File outputFile = new File("outputSRR.txt");
    private static OtherKerServices kernel;
    private static PrManager prmanager;
    private static long initialConfigTime;
    private static PrintWriter writer;


    public static void main(String[] args) throws FileNotFoundException {

        Scanner input = new Scanner(inputFile);
        writer = new PrintWriter(outputFile);
        String line = null;


        long nextExternalTime = Long.MAX_VALUE;
        long nextInternalTime = Long.MAX_VALUE;

        // ------------------------------------------------------------
        // 1. Read the CONFIG command (C ...)
        // ------------------------------------------------------------
        if (input.hasNext()) {
            line = input.nextLine();
            sysGen(line);
        }

        // ------------------------------------------------------------
        // 2. Read the FIRST external command (A or D)
        // ------------------------------------------------------------
        if (input.hasNext()) {
            line = input.nextLine();
            while (line.trim().isEmpty() && input.hasNext()) {  // skip empty lines
                line = input.nextLine();
            }
            nextExternalTime = extractTime(line);
        }

        // ------------------------------------------------------------
        // 3. PROCESS ALL ARRIVAL COMMANDS THAT OCCUR AT currentTime
        //    (very important for A 0, A 0, A 0 cases)
        // ------------------------------------------------------------
        while (nextExternalTime != Long.MAX_VALUE && nextExternalTime == currentTime) {

            // Handle this arrival or display command
            parseCmd(line);

            // Load next external command
            if (input.hasNext()) {
                line = input.nextLine();
                while (line.trim().isEmpty() && input.hasNext()) { // skip empties
                    line = input.nextLine();
                }
                nextExternalTime = extractTime(line);
            } else {
                line = null;
                nextExternalTime = Long.MAX_VALUE;
            }
        }

        // ------------------------------------------------------------
        // 4. After processing arrivals at t=0 (or config time),
        //    ADMIT EXACTLY ONE PROCESS FROM HOLDS into READY
        // ------------------------------------------------------------
        prmanager.admitOneFromHolds();

        // ------------------------------------------------------------
        // 5. First dispatch — now CPU receives the proper first job
        // ------------------------------------------------------------
        nextInternalTime = prmanager.dispatch(currentTime);


        // ------------------------------------------------------------
        // 6. MAIN SIMULATION LOOP
        // ------------------------------------------------------------
        while (nextExternalTime != Long.MAX_VALUE || nextInternalTime != Long.MAX_VALUE) {

            long nextTime = Math.min(nextExternalTime, nextInternalTime);
            long elapsed = nextTime - currentTime;
            currentTime = nextTime;

            // advance internal CPU clock
            prmanager.cpuTimeAdvance(elapsed);

            // INTERNAL event (slice end)
            if (currentTime == nextInternalTime) {
                prmanager.handleInternalEvent(currentTime);
                nextInternalTime = prmanager.getSliceEndTime();
            }

            // EXTERNAL event (arrival or display)
            else {
                parseCmd(line);

                if (input.hasNext()) {
                    line = input.nextLine();
                    while (line.trim().isEmpty() && input.hasNext()) {
                        line = input.nextLine();
                    }
                    nextExternalTime = extractTime(line);
                } else {
                    line = null;
                    nextExternalTime = Long.MAX_VALUE;
                }

                nextInternalTime = prmanager.dispatch(currentTime);
            }
        }

        input.close();
    }

    private static long extractTime(String line){
        String[] parts = line.split(" ");
        return Long.parseLong(parts[1]);
    }


    private static void handleArrival(String line){
        long arrivalTime = extractTime(line);
        long PID = Long.parseLong(line.split("=")[1].split(" ")[0]);
        long burstTime = Long.parseLong(line.split("=")[4].split(" ")[0]);
        long memoryReq = Long.parseLong(line.split("=")[2].split(" ")[0]);
        int noDevs = Integer.parseInt(line.split("=")[3].split(" ")[0]);
        int priority = Integer.parseInt(line.split("=")[5].split(" ")[0]);
        prmanager.procArrivingRoutine(PID, arrivalTime, burstTime, priority, memoryReq, noDevs);
        prmanager.dispatch(currentTime);
    }


    public static void sysGen(String line){
        // we know that any line here is a system config, we need to extract the information from that line and initialize the kernel.
        currentTime = extractTime(line);
        initialConfigTime = currentTime;
        long memorySize = Long.parseLong(line.split("=")[1].split(" ")[0]);
        int noDevs = Integer.parseInt(line.split("=")[2]);
        kernel = new OtherKerServices(memorySize, noDevs);
        prmanager = new PrManager(kernel);
        if(inputFile.getName().contains("SRR") || inputFile.getName().contains("srr")){
            prmanager.setScheduler(new SRoundRobinScheduler());
        }else {
            prmanager.setScheduler(new DRoundRobinScheduler());
        }
    }


    public static void parseCmd(String line){
        if (line == null || line.isEmpty()) return;

        char cmd = line.charAt(0);
        switch (cmd){
            case 'C':
                sysGen(line);
                break;
            case 'A':
                handleArrival(line);
                break;
            case 'D':
                printSystemStatus(line);
                break;
            default:
                System.out.println("Unkown command format!!");
                break;
        }

    }
    // ---------- DISPLAY HELPERS (SimulationController) ----------

    // convert numeric state to readable string
    private static String formatState(int s) {
        switch (s) {
            case 0: return "NEW";
            case 1: return "READY";
            case 2: return "RUNNING";
            case 3: return "HOLD";
            case 4: return "FINISHED";
            case 5: return "REJECTED";
            default: return "UNK";
        }
    }

    // format an entire queue into rows (header then each process)


    private static void printSystemStatus(String line) {
        double time = (double) extractTime(line);

        // Check which scheduler is active for the report header
        String schedulerName = inputFile.getName().toUpperCase().contains("SRR") ?
                "StaticRR" : "DynamicRR";

        writer.printf(Locale.US, "CONFIG at %.2f: mem=%d devices=%d scheduler=%s\n\n",
                (double) initialConfigTime,
                kernel.getSystemMemorySize(),
                kernel.getSystemNoDevs(),
                schedulerName // Dynamic name
        );

        writer.println("-------------------------------------------------------");
        writer.println("System Status:                                         ");
        writer.println("-------------------------------------------------------");
        writer.printf(Locale.US, "          Time: %.2f\n", time);
        writer.printf(Locale.US, "  Total Memory: %d\n", kernel.getSystemMemorySize());
        writer.printf(Locale.US, " Avail. Memory: %d\n", kernel.getCurrentMemorySize());
        writer.printf(Locale.US, " Total Devices: %d\n", kernel.getSystemNoDevs());
        writer.printf(Locale.US, "Avail. Devices: %d\n\n", kernel.getCurrentNoDevs());

        // Ready List
        writer.println("Jobs in Ready List                                      ");
        writer.println("--------------------------------------------------------");
        if (prmanager.getReadyQueue().isEmpty())
            writer.println("  EMPTY\n");
        else
            writer.print(prmanager.printReadyList());

        // Long Job List (always EMPTY for your design)
        writer.println("Jobs in Long Job List                                   ");
        writer.println("--------------------------------------------------------");
        writer.println("  EMPTY\n");

        // Hold 1
        writer.println("Jobs in Hold List 1                                     ");
        writer.println("--------------------------------------------------------");
        if (prmanager.getHoldQueue1().isEmpty())
            writer.println("  EMPTY\n");
        else
            writer.print(prmanager.printHold1());

        // Hold 2
        writer.println("Jobs in Hold List 2                                     ");
        writer.println("--------------------------------------------------------");
        if (prmanager.getHoldQueue2().isEmpty())
            writer.println("  EMPTY\n");
        else
            writer.print(prmanager.printHold2());

        // Finished jobs table
        writer.println("\nFinished Jobs (detailed)                                ");
        writer.println("--------------------------------------------------------");
        writer.println("  Job    ArrivalTime     CompleteTime     TurnaroundTime    WaitingTime");
        writer.println("------------------------------------------------------------------------");

        writer.print(prmanager.printFinishedJobs());

        writer.printf(Locale.US, "Total Finished Jobs:             %d\n\n\n",
                prmanager.getFinishedQueue().size());

        // Only print "Simulation finished" for the 999999 display command
        if (time >= 999999.0) {
            writer.printf(Locale.US, "--- Simulation finished at time %.1f ---\n",
                    (double) time);
        }

        // UPDATED: Flush the writer to ensure output is written immediately.
        writer.flush();
    }

}