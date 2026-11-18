import java.io.*;
import java.util.*;

// Custom unchecked business rule exceptions
class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String msg) { super(msg); }
}

class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String msg) { super(msg); }
}

public class ATMWithdrawal {
    private static double balance = 10000.0;

    public static void readCardFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Card-reader configuration file missing: " + filePath);
        }
        try (Scanner fileScanner = new Scanner(file)) {
            if (fileScanner.hasNextLine()) {
                System.out.println("[Card Reader] Verified card profile: " + fileScanner.nextLine());
            }
        }
    }

    public static void withdraw(String accountNo, String amountInput, String cardFile) {
        System.out.println("\n--- Processing ATM Transaction for Account: " + accountNo + " ---");
        try {
            if (accountNo == null || accountNo.trim().isEmpty()) {
                throw new IllegalArgumentException("Account number cannot be empty.");
            }
            // Checked exception point: card-reader file missing
            readCardFile(cardFile);

            // Unchecked exception point: NumberFormatException if user types letters
            double amount = Double.parseDouble(amountInput);

            // Unchecked business validation
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive. Entered: " + amount);
            }
            if (amount > balance) {
                throw new InsufficientBalanceException("Requested Rs " + amount + " exceeds balance Rs " + balance);
            }

            balance -= amount;
            System.out.println("[Success] Dispensed Rs " + amount + ". Remaining Balance: Rs " + balance);

        } catch (FileNotFoundException e) {
            System.out.println("[Caught Checked Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("[Caught Input Format Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("[Caught Input Format Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (InvalidAmountException | InsufficientBalanceException e) {
            // Multi-catch for domain rule violations
            System.out.println("[Caught Business Rule Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            // Broadest fallback catch placed last
            System.out.println("[Caught Fallback Exception] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            System.out.println("[Finally Block] ATM Card Ejected & Session Closed safely.");
        }
    }

    public static void main(String[] args) {
        // Create valid dummy card file
        try (PrintWriter pw = new PrintWriter("Q1/card_reader.txt")) {
            pw.println("CARD_ID=9876543210;STATUS=ACTIVE");
        } catch (IOException ignored) {}

        // 5+ test cases triggering distinct paths
        withdraw("ACC1001", "2500", "Q1/card_reader.txt");      // Test 1: Valid withdrawal
        withdraw("ACC1001", "ABC", "Q1/card_reader.txt");       // Test 2: NumberFormatException (letters)
        withdraw("ACC1001", "-500", "Q1/card_reader.txt");      // Test 3: InvalidAmountException (negative)
        withdraw("ACC1001", "50000", "Q1/card_reader.txt");     // Test 4: InsufficientBalanceException (> balance)
        withdraw("ACC1001", "1000", "Q1/missing_card.txt");     // Test 5: FileNotFoundException (missing file)
        withdraw("", "1000", "Q1/card_reader.txt");             // Test 6: IllegalArgumentException (blank account)
    }
}
