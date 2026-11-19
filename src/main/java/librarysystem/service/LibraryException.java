package librarysystem.service;

/** Checked exception for library operation and validation failures. */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
