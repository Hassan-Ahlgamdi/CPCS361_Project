# CPU Scheduling Simulator

## Project Overview

This project is a comprehensive discrete-event simulator for CPU scheduling, developed as a course project for **CPCS 361 (Operating Systems)**. The simulator models a multi-queue, resource-constrained operating system kernel to analyze and compare different process scheduling policies.

The core objective was to implement and validate two distinct scheduling algorithms:

1.  **Static Round Robin (SRR):** Uses a fixed time quantum (e.g., 14 units).
2.  **Dynamic Round Robin (DRR):** Uses an adaptive time quantum calculated dynamically based on the average remaining burst time of all processes in the ready queue.

## Features

* **Event-Driven Architecture:** Implements a precise event loop handling both external events (commands from input files) and internal events (CPU interrupts, quantum expiration).
* **Process Lifecycle Management:** Simulates the full lifecycle of a process: `NEW` → `HOLD` → `READY` → `RUNNING` → `FINISHED`.
* **Resource Management:** Models finite system resources (Memory and Devices). Processes are placed in specific **Hold Queues** (HQ1 for high priority, HQ2 for low priority) if resources are unavailable upon arrival.
* **Automated Testing:** Reads configuration and workload scenarios from input text files and generates detailed tabular reports.

## Project Structure

The project follows a modular, object-oriented design:

* `SimulationController`: The entry point and main engine driving the event loop.
* `PrManager`: The "kernel" responsible for process state transitions, queue management, and resource allocation.
* `Scheduler`: Abstract base class for scheduling policies.
    * `SRoundRobinScheduler`: Implementation of the static policy.
    * `DRoundRobinScheduler`: Implementation of the dynamic policy.
* `OtherKerServices`: Manages allocation/deallocation of Memory and I/O Devices.
* `Process`: Represents the Process Control Block (PCB).
* `Queue`: A generic queue wrapper with specialized sorting (e.g., MinMemory).

## Getting Started

### Prerequisites

* Java Development Kit (JDK) 8 or higher.
* An IDE (IntelliJ IDEA, Eclipse) or terminal for compilation.

### Usage

1.  **Configure Input:** Ensure your input files (e.g., `inputSRR.txt`, `inputDRR.txt`) are in the correct directory as referenced in `SimulationController.java`.
2.  **Run the Simulation:**
    Compile and run the `SimulationController` class.
    ```bash
    javac *.java
    java SimulationController
    ```
3.  **View Output:** The simulation will generate an `output.txt` file containing the system status at specific timestamps and a final table of finished jobs.

## Scheduling Algorithms

### Static Round Robin (SRR)

* **Logic:** Processes are dispatched in a FIFO order from the ready queue.
* **Quantum:** Fixed at **14 time units**.
* **Use Case:** Efficient for systems with predictable workloads where job burst times are generally close to the fixed quantum.

### Dynamic Round Robin (DRR)

* **Logic:** Processes are dispatched in FIFO order, but the time quantum is recalculated before every dispatch.
* **Quantum:** `Math.round(Sum of Remaining Bursts / Number of Processes)`.
* **Use Case:** Adaptable to varying workloads. It tends to favor shorter jobs when the queue is crowded (smaller quantum) and allows longer jobs to finish faster when the queue is empty (larger quantum).

## Results

The simulator has been validated against complex test cases involving memory constraints and device contention.

| Metric | SRR (Q=14) | DRR (Adaptive) |
| :--- | :--- | :--- |
| **Short Job (Burst < Q)** | Highly Efficient | Less Efficient (due to preemption) |
| **Long Job (Burst > Q)** | Standard Efficiency | Standard Efficiency |
| **Single Job in Queue** | Inefficient (Fixed Preemption) | **Optimal** (Quantum adjusts to Burst) |

## Authors

* Hassan Algamdi

