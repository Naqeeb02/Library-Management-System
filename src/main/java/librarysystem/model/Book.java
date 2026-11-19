package librarysystem.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/** Represents a book in the library. */
public class Book {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty author = new SimpleStringProperty();
    private final ObjectProperty<BookStatus> status = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> issueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> returnedDate = new SimpleObjectProperty<>();

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

    public LocalDate getIssueDate() { return issueDate.get(); }
    public void setIssueDate(LocalDate date) { issueDate.set(date); }
    public ObjectProperty<LocalDate> issueDateProperty() { return issueDate; }

    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate date) { dueDate.set(date); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    public LocalDate getReturnedDate() { return returnedDate.get(); }
    public void setReturnedDate(LocalDate date) { returnedDate.set(date); }
    public ObjectProperty<LocalDate> returnedDateProperty() { return returnedDate; }

    @Override
    public String toString() {
        return getId() + " - " + getTitle() + " (" + getAuthor() + ")";
    }
}
