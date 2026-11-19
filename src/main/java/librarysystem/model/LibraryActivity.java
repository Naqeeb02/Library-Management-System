package librarysystem.model;

import java.time.LocalDateTime;

/** Records a completed operation on a library book. */
public record LibraryActivity(String action, String bookId, String bookTitle, LocalDateTime occurredAt) {}
