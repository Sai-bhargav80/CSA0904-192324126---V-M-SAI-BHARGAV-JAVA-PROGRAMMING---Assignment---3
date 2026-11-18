import java.io.IOException;

// Checked Exceptions: recoverable / external boundary failures
class ExamPortalException extends Exception {
    public ExamPortalException(String message) { super(message); }
    public ExamPortalException(String message, Throwable cause) { super(message, cause); }
}

class ExamWindowClosedException extends ExamPortalException {
    public ExamWindowClosedException(String message) { super(message); }
}

class StorageLayerException extends ExamPortalException {
    public StorageLayerException(String message, Throwable cause) { super(message, cause); }
}

// Unchecked Exception: programming/precondition violation
class UnregisteredStudentException extends RuntimeException {
    public UnregisteredStudentException(String message) { super(message); }
}

// Layer 3: Storage Layer
class ExamStorageLayer {
    public void writeFile(String fileName, long sizeKb) throws StorageLayerException {
        try {
            if (sizeKb > 500) {
                throw new IOException("I/O Error: File size " + sizeKb + "KB exceeds 500KB limit.");
            }
            System.out.println("[Storage] Stored file: " + fileName);
        } catch (IOException ioEx) {
            // Chaining preserves root IOException
            throw new StorageLayerException("Storage failure while persisting " + fileName, ioEx);
        }
    }
}

// Layer 2: Service Layer
class ExamServiceLayer {
    private final ExamStorageLayer storage = new ExamStorageLayer();

    public void validateAndSubmit(String studentId, boolean registered, boolean windowOpen,
                                  String fileName, long sizeKb) throws ExamPortalException {
        if (!registered) {
            throw new UnregisteredStudentException("Student " + studentId + " is not enrolled in CSA09.");
        }
        if (!windowOpen) {
            throw new ExamWindowClosedException("Submission window for CSA09 is closed.");
        }
        // Propagates StorageLayerException upwards via 'throws'
        storage.writeFile(fileName, sizeKb);
    }
}

// Layer 1: Controller Layer
public class ExamPortalController {
    private final ExamServiceLayer service = new ExamServiceLayer();

    public void handleSubmission(String studentId, boolean registered, boolean windowOpen,
                                 String fileName, long sizeKb) {
        System.out.println("\n--- Submission Request: " + studentId + " | " + fileName + " ---");
        try {
            service.validateAndSubmit(studentId, registered, windowOpen, fileName, sizeKb);
            System.out.println("[UI Response] 200 OK: Submission accepted.");
        } catch (ExamWindowClosedException e) {
            System.out.println("[UI Response] 403 Forbidden: " + e.getMessage());
        } catch (StorageLayerException e) {
            System.out.println("[UI Response] 500 Storage Error: " + e.getMessage());
            System.out.println("[Internal Log] Root Cause -> " + e.getCause());
        } catch (ExamPortalException e) {
            System.out.println("[UI Response] 400 Portal Error: " + e.getMessage());
        } catch (UnregisteredStudentException e) {
            System.out.println("[UI Response] 401 Unauthorized: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ExamPortalController controller = new ExamPortalController();
        controller.handleSubmission("2311001", true, true, "script1.pdf", 250);   // Success
        controller.handleSubmission("2311002", false, true, "script2.pdf", 250);  // UnregisteredStudentException
        controller.handleSubmission("2311003", true, false, "script3.pdf", 250);  // ExamWindowClosedException
        controller.handleSubmission("2311004", true, true, "script4.pdf", 950);   // StorageLayerException (chained)
    }
}
