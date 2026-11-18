import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Q6 Part A & Alternative: Deadlock Prevention via Consistent Lock Ordering & ReentrantLock tryLock()
 */
public class HostelWalletTransfer {
    static class Wallet {
        final int id;
        double balance;
        final ReentrantLock lock = new ReentrantLock();

        Wallet(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    // Deadlock-free transfer using consistent ID lock ordering (breaks Circular Wait condition)
    public static void safeOrderedTransfer(Wallet from, Wallet to, double amount) {
        Wallet first = (from.id < to.id) ? from : to;
        Wallet second = (from.id < to.id) ? to : from;

        synchronized (first) {
            synchronized (second) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    // Alternative: tryLock() with timeout
    public static boolean tryLockTransfer(Wallet from, Wallet to, double amount) throws InterruptedException {
        if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
            try {
                if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        if (from.balance >= amount) {
                            from.balance -= amount;
                            to.balance += amount;
                            return true;
                        }
                    } finally {
                        to.lock.unlock();
                    }
                }
            } finally {
                from.lock.unlock();
            }
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        Wallet w1 = new Wallet(101, 5000);
        Wallet w2 = new Wallet(102, 5000);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) safeOrderedTransfer(w1, w2, 10);
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) safeOrderedTransfer(w2, w1, 10);
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        System.out.println("=== Deadlock-Free Lock-Ordered Transfers Complete ===");
        System.out.println("Wallet 101 Balance: Rs " + w1.balance + " | Wallet 102 Balance: Rs " + w2.balance);
        System.out.println("Total System Money Conserved: Rs " + (w1.balance + w2.balance));
    }
}
