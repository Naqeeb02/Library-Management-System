package librarysystem.service;

import librarysystem.model.Book;
import librarysystem.model.BookStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provides validated in-memory CRUD operations for library books. */
public class LibraryService {
    private final List<Book> books = new ArrayList<>();

    public synchronized Book addBook(String id, String title, String author) throws LibraryException {
        validateText(id, "Book ID");
        validateText(title, "Title");
        validateText(author, "Author");
        if (getBookById(id) != null) {
            throw new LibraryException("A book with ID '" + id.trim() + "' already exists.");
        }
        Book book = new Book(id.trim(), title.trim(), author.trim(), BookStatus.AVAILABLE);
        books.add(book);
        return book;
    }

    public synchronized List<Book> getAllBooks() {
        return Collections.unmodifiableList(new ArrayList<>(books));
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
        return book;
    }

    public synchronized boolean deleteBook(String id) throws LibraryException {
        validateText(id, "Book ID");
        Book book = getBookById(id);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + id.trim() + "'.");
        }
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
        book.setStatus(status);
        return book;
    }

    private void validateText(String value, String fieldName) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException(fieldName + " cannot be empty.");
        }
    }
}
