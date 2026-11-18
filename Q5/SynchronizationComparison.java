import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * Q5 Comparison: Unsynchronized vs Synchronized Method vs Synchronized Block vs AtomicBoolean vs ReentrantLock
 */
public class SynchronizationComparison {
    static class UnsyncBooking {
        final boolean[] booked;
        int count = 0;
        UnsyncBooking(int n) { booked = new boolean[n]; }
        void book(int s) {
            if (!booked[s]) {
                Thread.yield();
                booked[s] = true;
                count++;
            }
        }
    }

    static class BlockSyncBooking {
        final boolean[] booked;
        final Object lock = new Object();
        int count = 0;
        BlockSyncBooking(int n) { booked = new boolean[n]; }
        void book(int s) {
            // Non-critical logging could go outside
            synchronized (lock) {
                if (!booked[s]) {
                    booked[s] = true;
                    count++;
                }
            }
        }
    }

    static class AtomicBooking {
        final AtomicBoolean[] booked;
        final AtomicInteger count = new AtomicInteger(0);
        AtomicBooking(int n) {
            booked = new AtomicBoolean[n];
            for (int i = 0; i < n; i++) booked[i] = new AtomicBoolean(false);
        }
        void book(int s) {
            if (booked[s].compareAndSet(false, true)) {
                count.incrementAndGet();
            }
        }
    }

    static class LockBooking {
        final boolean[] booked;
        final ReentrantLock lock = new ReentrantLock();
        int count = 0;
        LockBooking(int n) { booked = new boolean[n]; }
        void book(int s) {
            lock.lock();
            try {
                if (!booked[s]) {
                    booked[s] = true;
                    count++;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. Demonstrate race condition over 5 runs
        System.out.println("=== Unsynchronized Race Condition Test (Expected: 10) ===");
        for (int run = 1; run <= 5; run++) {
            UnsyncBooking u = new UnsyncBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> u.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.println("Run " + run + " -> Seats sold (Unsync): " + u.count);
        }

        // 2. Benchmark 10,000 bookings across synchronization mechanisms
        final int SEATS = 10_000;
        BlockSyncBooking blockB = new BlockSyncBooking(SEATS);
        AtomicBooking atomicB = new AtomicBooking(SEATS);
        LockBooking lockB = new LockBooking(SEATS);

        long t1 = System.nanoTime();
        for (int i = 0; i < SEATS; i++) blockB.book(i);
        long blockTime = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        for (int i = 0; i < SEATS; i++) atomicB.book(i);
        long atomicTime = System.nanoTime() - t2;

        long t3 = System.nanoTime();
        for (int i = 0; i < SEATS; i++) lockB.book(i);
        long lockTime = System.nanoTime() - t3;

        System.out.println("\n=== 10,000 Bookings Throughput Comparison ===");
        System.out.printf("Synchronized Block : %.3f ms (Sold: %d)%n", blockTime / 1e6, blockB.count);
        System.out.printf("AtomicBoolean CAS  : %.3f ms (Sold: %d)%n", atomicTime / 1e6, atomicB.count.get());
        System.out.printf("ReentrantLock      : %.3f ms (Sold: %d)%n", lockTime / 1e6, lockB.count);
    }
}
