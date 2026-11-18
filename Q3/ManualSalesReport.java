import java.io.*;
import java.util.*;

/**
 * Q3 Part 1: Manual Resource Closing (Demonstrates file descriptor leak when mid-way exception occurs)
 */
public class ManualSalesReport {
    public static void generateReport(List<String> storeFiles, String summaryFile) throws IOException {
        List<BufferedReader> openedReaders = new ArrayList<>();
        BufferedWriter writer = null;
        double grandTotal = 0.0;

        try {
            writer = new BufferedWriter(new FileWriter(summaryFile));
            for (String path : storeFiles) {
                BufferedReader br = new BufferedReader(new FileReader(path));
                openedReaders.add(br); // Kept open until loop finishes -> LEAKS if an exception happens mid-loop!
                String line = br.readLine();
                if (line == null || line.startsWith("CORRUPT")) {
                    throw new IOException("Corrupt data encountered in file: " + path);
                }
                double sales = Double.parseDouble(line.trim());
                grandTotal += sales;
                writer.write(path + ": Rs " + sales + "\n");
            }
            writer.write("GRAND TOTAL: Rs " + grandTotal + "\n");
        } finally {
            // Buggy manual cleanup: if writer.close() throws or only partial readers are closed
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
            System.out.println("[Manual Version] Files left unclosed inside loop before failure: " + openedReaders.size());
            for (BufferedReader r : openedReaders) {
                try { r.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new File("Q3/data").mkdirs();
        try (PrintWriter p1 = new PrintWriter("Q3/data/store1.txt");
             PrintWriter p2 = new PrintWriter("Q3/data/store2.txt");
             PrintWriter p3 = new PrintWriter("Q3/data/store3.txt")) {
            p1.println("15000.50");
            p2.println("CORRUPT_RECORD");
            p3.println("22000.00");
        }

        List<String> files = Arrays.asList("Q3/data/store1.txt", "Q3/data/store2.txt", "Q3/data/store3.txt");
        try {
            generateReport(files, "Q3/manual_summary.txt");
        } catch (IOException e) {
            System.out.println("[Manual Run Failed] " + e.getMessage());
        }
    }
}
