package librarysystem.service;

import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {
    private LibraryService service;

    @BeforeEach
    void setUp() {
        service = new LibraryService();
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
    void changesStatus() throws LibraryException {
        service.addBook("B001", "Title", "Author");
        Book book = service.updateBookStatus("B001", BookStatus.ISSUED);
        assertEquals(BookStatus.ISSUED, book.getStatus());
        assertThrows(LibraryException.class,
                () -> service.updateBookStatus("B001", null));
    }
}
