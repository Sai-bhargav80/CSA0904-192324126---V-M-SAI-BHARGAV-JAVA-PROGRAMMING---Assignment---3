import java.util.Scanner;

/**
 * Task Q1 Alternative: Defensive Pre-checking with Error Codes (No Exceptions) vs Exception Handling.
 */
public class ATMErrorCodeAlternative {
    public static final int SUCCESS = 0;
    public static final int ERR_INVALID_INPUT = -1;
    public static final int ERR_NEGATIVE_AMOUNT = -2;
    public static final int ERR_INSUFFICIENT_FUNDS = -3;

    private static double balance = 10000.0;

    public static int withdrawWithErrorCodes(String input) {
        Scanner sc = new Scanner(input);
        if (!sc.hasNextDouble()) {
            sc.close();
            return ERR_INVALID_INPUT;
        }
        double amount = sc.nextDouble();
        sc.close();

        if (amount <= 0) {
            return ERR_NEGATIVE_AMOUNT;
        }
        if (amount > balance) {
            return ERR_INSUFFICIENT_FUNDS;
        }
        balance -= amount;
        return SUCCESS;
    }

    public static void main(String[] args) {
        final int ITERATIONS = 100_000;

        // Measure Error-Code approach on invalid input
        long startErr = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            int code = withdrawWithErrorCodes("INVALID");
            if (code != SUCCESS) { /* handled */ }
        }
        long errTime = System.nanoTime() - startErr;

        // Measure Exception Throwing + Stack Trace Capture approach on invalid input
        long startEx = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            try {
                Double.parseDouble("INVALID");
            } catch (NumberFormatException e) {
                /* handled */
            }
        }
        long exTime = System.nanoTime() - startEx;

        System.out.println("=== Q1 Benchmark: Error Codes vs Exceptions (100,000 invalid inputs) ===");
        System.out.printf("Error-Code Pre-check Time : %.3f ms%n", errTime / 1_000_000.0);
        System.out.printf("Exception Throw/Catch Time: %.3f ms%n", exTime / 1_000_000.0);
    }
}
