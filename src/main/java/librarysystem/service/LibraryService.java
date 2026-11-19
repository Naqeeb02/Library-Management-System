package librarysystem.service;

import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import librarysystem.model.LibraryActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Provides validated in-memory CRUD operations for library books. */
public class LibraryService {
    private static final int MAX_RECENT_ACTIVITIES = 30;
    public static final long DAILY_OVERDUE_FINE_PKR = 50;
    private final List<Book> books = new ArrayList<>();
    private final List<LibraryActivity> activities = new ArrayList<>();
    private final Clock clock;

    public LibraryService() {
        this(Clock.systemDefaultZone());
    }

    public LibraryService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Book addBook(String id, String title, String author) throws LibraryException {
        validateText(id, "Book ID");
        validateText(title, "Title");
        validateText(author, "Author");
        if (getBookById(id) != null) {
            throw new LibraryException("A book with ID '" + id.trim() + "' already exists.");
        }
        Book book = new Book(id.trim(), title.trim(), author.trim(), BookStatus.AVAILABLE);
        books.add(book);
        recordActivity("Added", book);
        return book;
    }

    public synchronized List<Book> getAllBooks() {
        return Collections.unmodifiableList(new ArrayList<>(books));
    }

    /** Returns recent book operations, newest first. */
    public synchronized List<LibraryActivity> getRecentActivities() {
        List<LibraryActivity> recent = new ArrayList<>(activities);
        Collections.reverse(recent);
        return Collections.unmodifiableList(recent);
    }

    public synchronized Book getBookById(String id) {
        if (id == null) return null;
        String normalized = id.trim();
        return books.stream()
                .filter(book -> book.getId().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public synchronized Book updateBook(String id, String title, String author) throws LibraryException {
        validateText(id, "Book ID");
        validateText(title, "Title");
        validateText(author, "Author");
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        recordActivity("Updated", book);
        return book;
    }

    public synchronized boolean deleteBook(String id) throws LibraryException {
        validateText(id, "Book ID");
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        recordActivity("Deleted", book);
        return books.remove(book);
    }

    public synchronized Book updateBookStatus(String id, BookStatus status) throws LibraryException {
        validateText(id, "Book ID");
        if (status == null) {
            throw new LibraryException("A valid status must be selected.");
        }
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        if (status == BookStatus.ISSUED) {
            throw new LibraryException("Use the Issue Book action to enter a return-by date.");
        }
        if (book.getStatus() == BookStatus.ISSUED && status != BookStatus.ISSUED) {
            throw new LibraryException("Return the issued book before changing its status.");
        }
        if (book.getStatus() != status) {
            recordActivity("Status changed to " + status.name(), book);
        }
        book.setStatus(status);
        return book;
    }

    /** Issues a book and records the issue date and required return-by date. */
    public synchronized Book issueBook(String id, LocalDate dueDate) throws LibraryException {
        validateText(id, "Book ID");
        if (dueDate == null) {
            throw new LibraryException("A return-by date is required.");
        }
        LocalDate issueDate = LocalDate.now(clock);
        if (dueDate.isBefore(issueDate)) {
            throw new LibraryException("The return-by date cannot be before the issue date.");
        }
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        if (book.getStatus() == BookStatus.ISSUED) {
            throw new LibraryException("This book is already issued. Return it before issuing again.");
        }
        book.setIssueDate(issueDate);
        book.setDueDate(dueDate);
        book.setReturnedDate(null);
        book.setStatus(BookStatus.ISSUED);
        recordActivity("Issued; due " + dueDate, book);
        return book;
    }

    /** Returns an issued book and calculates its final overdue fine in PKR. */
    public synchronized long returnBook(String id) throws LibraryException {
        validateText(id, "Book ID");
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        if (book.getStatus() != BookStatus.ISSUED || book.getDueDate() == null) {
            throw new LibraryException("This book is not currently issued.");
        }
        LocalDate returnDate = LocalDate.now(clock);
        long fine = calculateOverdueFine(book, returnDate);
        book.setReturnedDate(returnDate);
        book.setStatus(BookStatus.AVAILABLE);
        recordActivity(fine > 0
                ? "Returned; overdue fine PKR " + fine
                : "Returned on time; no fine", book);
        return fine;
    }

    /** Calculates the currently accrued fine for an issued book in PKR. */
    public synchronized long getOverdueFine(String id) throws LibraryException {
        validateText(id, "Book ID");
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
        if (book.getStatus() != BookStatus.ISSUED) {
            return 0;
        }
        return calculateOverdueFine(book, LocalDate.now(clock));
    }

    private long calculateOverdueFine(Book book, LocalDate date) {
        if (book.getDueDate() == null || !date.isAfter(book.getDueDate())) {
            return 0;
        }
        long overdueDays = ChronoUnit.DAYS.between(book.getDueDate(), date);
        return overdueDays * DAILY_OVERDUE_FINE_PKR;
    }

    private void recordActivity(String action, Book book) {
        activities.add(new LibraryActivity(action, book.getId(), book.getTitle(),
                LocalDateTime.now(clock)));
        if (activities.size() > MAX_RECENT_ACTIVITIES) {
            activities.remove(0);
        }
    }

    private void validateText(String value, String fieldName) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException(fieldName + " cannot be empty.");
        }
    }
}
