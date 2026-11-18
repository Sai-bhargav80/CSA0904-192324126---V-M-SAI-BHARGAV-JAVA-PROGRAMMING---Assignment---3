import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Q7: Concurrent, Fault-Tolerant Semester Result Processor (10,000 files benchmark + fault injection)
 */
public class SemesterResultProcessor {

    static class StudentResult {
        final String studentId;
        final int totalMarks;
        final String grade;

        StudentResult(String studentId, int totalMarks, String grade) {
            this.studentId = studentId;
            this.totalMarks = totalMarks;
            this.grade = grade;
        }
    }

    static class FileTask implements Callable<StudentResult> {
        private final Path filePath;

        FileTask(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public StudentResult call() throws Exception {
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("Missing score file: " + filePath.getFileName());
            }
            List<String> lines = Files.readAllLines(filePath);
            if (lines.isEmpty()) {
                throw new IOException("Empty/corrupt file: " + filePath.getFileName());
            }
            // Format: STUDENT_ID,M1,M2,M3
            String[] parts = lines.get(0).split(",");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Malformed record in " + filePath.getFileName());
            }
            String id = parts[0].trim();
            int m1 = Integer.parseInt(parts[1].trim());
            int m2 = Integer.parseInt(parts[2].trim());
            int m3 = Integer.parseInt(parts[3].trim());
            if (m1 < 0 || m1 > 100 || m2 < 0 || m2 > 100 || m3 < 0 || m3 > 100) {
                throw new IllegalArgumentException("Out-of-range marks in " + filePath.getFileName());
            }
            int total = m1 + m2 + m3;
            String grade = (total >= 240) ? "O" : (total >= 180) ? "A" : (total >= 150) ? "B" : "F";
            return new StudentResult(id, total, grade);
        }
    }

    public static long runWithPoolSize(List<Path> files, int poolSize, boolean printSummary) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentHashMap<String, StudentResult> resultsMap = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<String> errorLog = new ConcurrentLinkedQueue<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long start = System.nanoTime();
        List<Future<StudentResult>> futures = new ArrayList<>(files.size());
        for (Path p : files) {
            futures.add(executor.submit(new FileTask(p)));
        }

        for (int i = 0; i < futures.size(); i++) {
            try {
                StudentResult res = futures.get(i).get();
                resultsMap.put(res.studentId, res);
                successCount.incrementAndGet();
            } catch (ExecutionException e) {
                failureCount.incrementAndGet();
                errorLog.add(files.get(i).getFileName() + " -> " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        if (printSummary) {
            System.out.println("=== Semester Result Summary (Pool Size = " + poolSize + ") ===");
            System.out.println("Total Files Scheduled : " + files.size());
            System.out.println("Processed Successfully: " + successCount.get());
            System.out.println("Failed / Corrupt Files: " + failureCount.get());
            System.out.println("Sample Error Logs     : ");
            errorLog.stream().limit(3).forEach(err -> System.out.println("   - " + err));
        }
        return elapsedMs;
    }

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("sem_results_10k");
        int totalFiles = 10_000;
        List<Path> filePaths = new ArrayList<>(totalFiles);

        // Generate 10,000 files with injected missing and corrupt records
        for (int i = 1; i <= totalFiles; i++) {
            Path p = tempDir.resolve("student_" + i + ".txt");
            filePaths.add(p);
            if (i % 2000 == 0) {
                // Inject missing file (do not create on disk)
                continue;
            } else if (i % 1500 == 0) {
                // Inject corrupt data
                Files.writeString(p, "STU" + i + ",95,CORRUPT,88");
            } else {
                Files.writeString(p, "STU" + i + ",85,78,90");
            }
        }

        // Benchmark across thread pool sizes 1, 2, 4, 8
        int[] poolSizes = {1, 2, 4, 8};
        System.out.println("=== Scaling Benchmark across 10,000 Score Files ===");
        for (int pools : poolSizes) {
            long timeMs = runWithPoolSize(filePaths, pools, pools == 4);
            System.out.printf("Threads: %d -> Total Time: %d ms%n", pools, timeMs);
        }

        // Parallel Stream alternative measurement
        long pStart = System.nanoTime();
        long validParallel = filePaths.parallelStream().filter(Files::exists).count();
        long pTime = (System.nanoTime() - pStart) / 1_000_000;
        System.out.printf("Alternative parallelStream() existing file check: %d ms (Existing: %d)%n", pTime, validParallel);

        // Cleanup temp files
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
}
