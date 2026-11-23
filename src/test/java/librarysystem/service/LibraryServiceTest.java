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
        Book book = service.addBook("ACL-001", "Clean Code", "Robert Martin");
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    void rejectsEmptyFieldsAndDuplicateIds() {
        assertThrows(LibraryException.class,
                () -> service.addBook("", "Title", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("ACL-001", "", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("ACL-001", "Title", ""));

        assertDoesNotThrow(() -> service.addBook("ACL-001", "Title", "Author"));
        assertThrows(LibraryException.class,
                () -> service.addBook("ACL-001", "Another", "Another Author"));
    }

    @Test
    void rejectsDuplicateBookTitles() throws LibraryException {
        service.addBook("ACL-001", "Naqeeb", "Author One");
        LibraryException duplicate = assertThrows(LibraryException.class,
                () -> service.addBook("ACL-002", "Naqeeb", "Author Two"));
        assertTrue(duplicate.getMessage().contains("title"));

        LibraryException caseInsensitive = assertThrows(LibraryException.class,
                () -> service.addBook("ACL-003", "naqeeb", "Another Author"));
        assertTrue(caseInsensitive.getMessage().contains("title"));

        service.addBook("ACL-004", "Different Title", "Author One");
        assertThrows(LibraryException.class,
                () -> service.updateBook("ACL-004", "Naqeeb", "Author One"));
        assertDoesNotThrow(() -> service.updateBook("ACL-001", "Naqeeb", "Updated Author"));
    }

    @Test
    void rejectsInvalidBookIdTitleAndAuthorFormats() {
        LibraryException invalidId = assertThrows(LibraryException.class,
                () -> service.addBook("B001", "Clean Code", "Robert Martin"));
        assertTrue(invalidId.getMessage().contains("ACL-"));

        assertThrows(LibraryException.class,
                () -> service.addBook("ACL-", "Clean Code", "Robert Martin"));

        assertDoesNotThrow(() -> service.addBook("ACL-001", "Clean Code 2", "Robert Martin"));
        LibraryException titleStartsWithNumber = assertThrows(LibraryException.class,
                () -> service.addBook("ACL-002", "2 Clean Code", "Robert Martin"));
        assertTrue(titleStartsWithNumber.getMessage().contains("start with a letter"));
        LibraryException titleSpecial = assertThrows(LibraryException.class,
                () -> service.addBook("ACL-003", "Clean-Code", "Robert Martin"));
        assertTrue(titleSpecial.getMessage().contains("letter"));

        LibraryException invalidAuthor = assertThrows(LibraryException.class,
                () -> service.addBook("ACL-004", "Clean Code", "Robert Martin 2"));
        assertTrue(invalidAuthor.getMessage().contains("letters"));
        assertThrows(LibraryException.class,
                () -> service.addBook("ACL-005", "Clean Code", "Robert-Martin"));
    }

    @Test
    void viewsEmptyAndPopulatedLibrary() throws LibraryException {
        assertTrue(service.getAllBooks().isEmpty());
        service.addBook("ACL-001", "Title", "Author");
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    void updatesExistingBookAndRejectsMissingBook() throws LibraryException {
        service.addBook("ACL-001", "Old Title", "Old Author");
        Book updated = service.updateBook("ACL-001", "New Title", "New Author");
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Author", updated.getAuthor());
        assertThrows(LibraryException.class,
                () -> service.updateBook("ACL-MISSING", "Title", "Author"));
    }

    @Test
    void deletesExistingBookAndRejectsMissingBook() throws LibraryException {
        service.addBook("ACL-001", "Title", "Author");
        assertTrue(service.deleteBook("ACL-001"));
        assertTrue(service.getAllBooks().isEmpty());
        assertThrows(LibraryException.class, () -> service.deleteBook("ACL-MISSING"));
    }

    @Test
    void rejectsDeletingIssuedOrReservedBook() throws LibraryException {
        service.addBook("ACL-001", "Title", "Author");
        service.updateBookStatus("ACL-001", BookStatus.RESERVED);
        LibraryException reserved = assertThrows(LibraryException.class,
                () -> service.deleteBook("ACL-001"));
        assertTrue(reserved.getMessage().toLowerCase().contains("available"));

        service.updateBookStatus("ACL-001", BookStatus.AVAILABLE);
        service.issueBook("ACL-001", LocalDate.of(2025, 1, 5));
        LibraryException issued = assertThrows(LibraryException.class,
                () -> service.deleteBook("ACL-001"));
        assertTrue(issued.getMessage().toLowerCase().contains("available"));

        service.returnBook("ACL-001");
        assertTrue(service.deleteBook("ACL-001"));
    }

    @Test
    void issuesBookWithReturnByDate() throws LibraryException {
        service.addBook("ACL-001", "Title", "Author");
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("ACL-001", BookStatus.ISSUED));
        Book book = service.issueBook("ACL-001", LocalDate.of(2025, 1, 5));
        assertEquals(BookStatus.ISSUED, book.getStatus());
        assertEquals(LocalDate.of(2025, 1, 1), book.getIssueDate());
        assertEquals(LocalDate.of(2025, 1, 5), book.getDueDate());
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("ACL-001", BookStatus.AVAILABLE));
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("ACL-001", null));
    }

    @Test
    void calculatesDailyFineAndReturnsBook() throws LibraryException {
        service.addBook("ACL-001", "Title", "Author");
        service.issueBook("ACL-001", LocalDate.of(2025, 1, 3));

        clock.setDate(LocalDate.of(2025, 1, 3));
        assertEquals(0, service.getOverdueFine("ACL-001"));
        clock.setDate(LocalDate.of(2025, 1, 4));
        assertEquals(50, service.getOverdueFine("ACL-001"));
        clock.setDate(LocalDate.of(2025, 1, 6));
        assertEquals(150, service.returnBook("ACL-001"));
        assertEquals(BookStatus.AVAILABLE, service.getBookById("ACL-001").getStatus());
        assertEquals(LocalDate.of(2025, 1, 6), service.getBookById("ACL-001").getReturnedDate());
        assertEquals(0, service.getOverdueFine("ACL-001"));
    }

    @Test
    void rejectsPastDueDateAndReturningANonIssuedBook() throws LibraryException {
        service.addBook("ACL-001", "Title", "Author");
        assertThrows(LibraryException.class,
                () -> service.issueBook("ACL-001", LocalDate.of(2024, 12, 31)));
        assertThrows(LibraryException.class, () -> service.returnBook("ACL-001"));
        service.issueBook("ACL-001", LocalDate.of(2025, 1, 2));
        assertThrows(LibraryException.class,
                () -> service.issueBook("ACL-001", LocalDate.of(2025, 1, 4)));
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
        service.addBook("ACL-001", "Title", "Author");
        service.updateBookStatus("ACL-001", BookStatus.RESERVED);
        service.updateBook("ACL-001", "Updated Title", "Author");

        assertEquals(3, service.getRecentActivities().size());
        assertEquals("Updated", service.getRecentActivities().get(0).action());
        assertEquals("Status changed to RESERVED", service.getRecentActivities().get(1).action());
        assertEquals("Added", service.getRecentActivities().get(2).action());
    }
}
