import java.util.concurrent.*;

/**
 * Q4: Thread Creation (Extending Thread vs Implementing Runnable), Life Cycle, Daemon & ExecutorService Alternative
 */
class ScannerStationThread extends Thread {
    private final int scriptsToScan;

    public ScannerStationThread(String name, int scriptsToScan) {
        super(name);
        this.scriptsToScan = scriptsToScan;
    }

    @Override
    public void run() {
        System.out.println("[" + getName() + "] State: RUNNABLE | Thread: " + Thread.currentThread().getName());
        for (int i = 1; i <= scriptsToScan; i++) {
            try {
                Thread.sleep(20); // TIMED_WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("[" + getName() + "] Completed scanning " + scriptsToScan + " scripts.");
    }
}

class ScannerStationRunnable implements Runnable {
    private final String stationName;
    private final int scriptsToScan;
    private int scannedCount = 0;

    public ScannerStationRunnable(String stationName, int scriptsToScan) {
        this.stationName = stationName;
        this.scriptsToScan = scriptsToScan;
    }

    public int getScannedCount() { return scannedCount; }

    @Override
    public void run() {
        for (int i = 1; i <= scriptsToScan; i++) {
            try {
                Thread.sleep(15);
                scannedCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("[" + stationName + "] Runnable finished " + scannedCount + " scripts.");
    }
}

public class AnswerScriptScannerDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: Extending Thread & Life Cycle States ===");
        ScannerStationThread s1 = new ScannerStationThread("Station-1", 5);
        ScannerStationThread s2 = new ScannerStationThread("Station-2", 5);
        System.out.println("Before start() -> s1.isAlive(): " + s1.isAlive() + ", State: " + s1.getState());

        // Direct run() vs start() evidence
        System.out.println("Calling s1.run() directly executes on thread: " + Thread.currentThread().getName());

        s1.start();
        s2.start();
        System.out.println("After start() -> s1.isAlive(): " + s1.isAlive() + ", State: " + s1.getState());

        s1.join();
        s2.join();
        System.out.println("After join() -> s1.isAlive(): " + s1.isAlive() + ", State: " + s1.getState());

        System.out.println("\n=== Part 2: Implementing Runnable (4 Stations) ===");
        ScannerStationRunnable[] tasks = new ScannerStationRunnable[4];
        Thread[] workers = new Thread[4];
        for (int i = 0; i < 4; i++) {
            tasks[i] = new ScannerStationRunnable("Runnable-Station-" + (i + 1), 10);
            workers[i] = new Thread(tasks[i]);
            workers[i].setPriority((i == 3) ? Thread.MAX_PRIORITY : Thread.NORM_PRIORITY);
            workers[i].start();
        }
        for (Thread w : workers) w.join();

        int total = 0;
        for (ScannerStationRunnable t : tasks) total += t.getScannedCount();
        System.out.println("Total Scripts Scanned across 4 stations: " + total);

        System.out.println("\n=== Part 3: ExecutorService Fixed Thread Pool Alternative ===");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 1; i <= 4; i++) {
            final int id = i;
            pool.submit(() -> System.out.println("[Pool Worker] Station-" + id + " handled by " + Thread.currentThread().getName()));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
