package librarysystem.service;

import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {
    private LibraryService service;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(LocalDate.of(2025, 1, 1));
        service = new LibraryService(clock);
    }

    @Test
    void addsValidBook() throws LibraryException {
        Book book = service.addBook("B001", "Clean Code", "Robert Martin");
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    void rejectsEmptyFieldsAndDuplicateIds() {
        assertThrows(LibraryException.class,
                () -> service.addBook("", "Title", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("B001", "", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("B001", "Title", ""));

        assertDoesNotThrow(() -> service.addBook("B001", "Title", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("B001", "Another", "Another Author"));
    }

    @Test
    void viewsEmptyAndPopulatedLibrary() throws LibraryException {
        assertTrue(service.getAllBooks().isEmpty());
        service.addBook("B001", "Title", "Author");
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    void updatesExistingBookAndRejectsMissingBook() throws LibraryException {
        service.addBook("B001", "Old Title", "Old Author");
        Book updated = service.updateBook("B001", "New Title", "New Author");
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Author", updated.getAuthor());
        assertThrows(LibraryException.class,
                () -> service.updateBook("MISSING", "Title", "Author"));
    }

    @Test
    void deletesExistingBookAndRejectsMissingBook() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        assertTrue(service.deleteBook("B001"));
        assertTrue(service.getAllBooks().isEmpty());
        assertThrows(LibraryException.class, () -> service.deleteBook("MISSING"));
    }

    @Test
    void issuesBookWithReturnByDate() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("B001", BookStatus.ISSUED));
        Book book = service.issueBook("B001", LocalDate.of(2025, 1, 5));
        assertEquals(BookStatus.ISSUED, book.getStatus());
        assertEquals(LocalDate.of(2025, 1, 1), book.getIssueDate());
        assertEquals(LocalDate.of(2025, 1, 5), book.getDueDate());
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("B001", BookStatus.AVAILABLE));
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("B001", null));
    }

    @Test
    void calculatesDailyFineAndReturnsBook() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        service.issueBook("B001", LocalDate.of(2025, 1, 3));

        clock.setDate(LocalDate.of(2025, 1, 3));
        assertEquals(0, service.getOverdueFine("B001"));
        clock.setDate(LocalDate.of(2025, 1, 4));
        assertEquals(50, service.getOverdueFine("B001"));
        clock.setDate(LocalDate.of(2025, 1, 6));
        assertEquals(150, service.returnBook("B001"));
        assertEquals(BookStatus.AVAILABLE, service.getBookById("B001").getStatus());
        assertEquals(LocalDate.of(2025, 1, 6), service.getBookById("B001").getReturnedDate());
        assertEquals(0, service.getOverdueFine("B001"));
    }

    @Test
    void rejectsPastDueDateAndReturningANonIssuedBook() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        assertThrows(LibraryException.class,
                () -> service.issueBook("B001", LocalDate.of(2024, 12, 31)));
        assertThrows(LibraryException.class, () -> service.returnBook("B001"));
        service.issueBook("B001", LocalDate.of(2025, 1, 2));
        assertThrows(LibraryException.class,
                () -> service.issueBook("B001", LocalDate.of(2025, 1, 4)));
    }

    private static final class MutableClock extends Clock {
        private final ZoneId zone = ZoneId.of("UTC");
        private Instant instant;

        private MutableClock(LocalDate date) {
            setDate(date);
        }

        private void setDate(LocalDate date) {
            instant = date.atStartOfDay(zone).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    void recordsRecentOperationsNewestFirst() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        service.updateBookStatus("B001", BookStatus.RESERVED);
        service.updateBook("B001", "Updated title", "Author");

        assertEquals(3, service.getRecentActivities().size());
        assertEquals("Updated", service.getRecentActivities().get(0).action());
        assertEquals("Status changed to RESERVED", service.getRecentActivities().get(1).action());
        assertEquals("Added", service.getRecentActivities().get(2).action());
    }
}
