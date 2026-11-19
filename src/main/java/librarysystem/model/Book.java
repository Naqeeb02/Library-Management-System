package librarysystem.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Represents a book in the library. */
public class Book {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty author = new SimpleStringProperty();
    private final ObjectProperty<BookStatus> status = new SimpleObjectProperty<>();

    public Book(String id, String title, String author) {
        this(id, title, author, BookStatus.AVAILABLE);
    }

    public Book(String id, String title, String author, BookStatus status) {
        setId(id);
        setTitle(title);
        setAuthor(author);
        setStatus(status);
    }

    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }
    public StringProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String title) { this.title.set(title); }
    public StringProperty titleProperty() { return title; }

    public String getAuthor() { return author.get(); }
    public void setAuthor(String author) { this.author.set(author); }
    public StringProperty authorProperty() { return author; }

    public BookStatus getStatus() { return status.get(); }
    public void setStatus(BookStatus status) { this.status.set(status); }
    public ObjectProperty<BookStatus> statusProperty() { return status; }

    @Override
    public String toString() {
        return getId() + " - " + getTitle() + " (" + getAuthor() + ")";
    }
}
