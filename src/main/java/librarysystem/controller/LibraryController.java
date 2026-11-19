package librarysystem.controller;

import javafx.scene.control.Alert;
import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import librarysystem.service.LibraryException;
import librarysystem.service.LibraryService;
import librarysystem.view.MainView;

/** Connects the dashboard and book-management actions to the library service. */
public class LibraryController {
    private final LibraryService service;
    private final MainView view;

    /** Creates the controller and connects view actions to service operations. */
    public LibraryController(LibraryService service, MainView view) {
        this.service = service;
        this.view = view;
        view.setAddHandler(this::addBook);
        view.setEditHandler(this::editBook);
        view.setDeleteHandler(this::deleteBook);
        view.setDetailsHandler(this::handleDetailsAction);
        view.setStatusHandler(this::changeStatus);
        refresh();
    }

    private void addBook() {
        view.showBookDialog(null, data -> {
            try {
                service.addBook(data.id(), data.title(), data.author());
                refresh();
                view.showMessage(Alert.AlertType.INFORMATION, "Book added",
                        "The book was added to your collection.");
            } catch (LibraryException exception) {
                showError(exception);
            }
        });
    }

    private void editBook(Book book) {
        view.showBookDialog(book, data -> {
            try {
                service.updateBook(book.getId(), data.title(), data.author());
                refresh();
                view.showMessage(Alert.AlertType.INFORMATION, "Book updated",
                        "The book details were saved.");
            } catch (LibraryException exception) {
                showError(exception);
            }
        });
    }

    private void deleteBook(Book book) {
        if (!view.confirmDelete(book)) {
            return;
        }
        try {
            service.deleteBook(book.getId());
            refresh();
            view.showMessage(Alert.AlertType.INFORMATION, "Book deleted",
                    "The book was removed from your collection.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void changeStatus(Book book, BookStatus status) {
        if (status == BookStatus.ISSUED) {
            view.showIssueDialog(book, dueDate -> issueBook(book, dueDate));
            return;
        }
        try {
            service.updateBookStatus(book.getId(), status);
            refresh();
            view.showMessage(Alert.AlertType.INFORMATION, "Status updated",
                    "The status for \"" + book.getTitle() + "\" is now " + status + ".");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void issueBook(Book book, java.time.LocalDate dueDate) {
        try {
            service.issueBook(book.getId(), dueDate);
            refresh();
            view.showMessage(Alert.AlertType.INFORMATION, "Book issued",
                    "\"" + book.getTitle() + "\" is due on " + dueDate
                            + ". Late returns are fined PKR "
                            + LibraryService.DAILY_OVERDUE_FINE_PKR + " per day.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void returnBook(Book book) {
        if (!view.confirmReturn(book)) {
            return;
        }
        try {
            long fine = service.returnBook(book.getId());
            refresh();
            String message = fine == 0
                    ? "The book was returned on time. No fine is due."
                    : "The book was returned late. Fine due: PKR " + fine + ".";
            view.showMessage(Alert.AlertType.INFORMATION, "Book returned", message);
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void handleDetailsAction(Book book, MainView.BookAction action) {
        switch (action) {
            case VIEW_DETAILS -> {
                try {
                    view.showBookDetails(book, service.getOverdueFine(book.getId()));
                } catch (LibraryException exception) {
                    showError(exception);
                }
            }
            case EDIT -> editBook(book);
            case CHANGE_STATUS -> view.showStatusDialog(book,
                    status -> changeStatus(book, status));
            case DELETE -> deleteBook(book);
            case RETURN_BOOK -> returnBook(book);
        }
    }

    private void refresh() {
        view.setBooks(service.getAllBooks());
        view.setActivities(service.getRecentActivities());
    }

    private void showError(LibraryException exception) {
        view.showMessage(Alert.AlertType.ERROR, "Unable to save changes", exception.getMessage());
    }
}
