import java.io.*;
import java.util.*;

/**
 * Q3 Part 2: Safe Try-With-Resources Implementation (Zero leaked handles + Suppressed exception support)
 */
public class SafeSalesReportTWR {
    public static void generateReport(List<String> storeFiles, String summaryFile) throws IOException {
        double grandTotal = 0.0;

        // Writer automatically closed at block exit
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryFile))) {
            for (String path : storeFiles) {
                // Each store reader is scoped to a single iteration and closed immediately via TWR
                try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                    String line = br.readLine();
                    if (line == null || line.startsWith("CORRUPT")) {
                        throw new IOException("Corrupt data encountered in file: " + path);
                    }
                    double sales = Double.parseDouble(line.trim());
                    grandTotal += sales;
                    writer.write(path + ": Rs " + sales + "\n");
                } // br.close() guaranteed here even if IOException is thrown!
            }
            writer.write("GRAND TOTAL: Rs " + grandTotal + "\n");
        }
    }

    public static void main(String[] args) throws IOException {
        new File("Q3/data").mkdirs();
        try (PrintWriter p1 = new PrintWriter("Q3/data/store1.txt");
             PrintWriter p2 = new PrintWriter("Q3/data/store2.txt");
             PrintWriter p3 = new PrintWriter("Q3/data/store3.txt")) {
            p1.println("15000.50");
            p2.println("18500.75");
            p3.println("22000.00");
        }

        List<String> validFiles = Arrays.asList("Q3/data/store1.txt", "Q3/data/store2.txt", "Q3/data/store3.txt");
        generateReport(validFiles, "Q3/twr_summary.txt");
        System.out.println("[TWR Run Success] Summary written to Q3/twr_summary.txt");
    }
}
