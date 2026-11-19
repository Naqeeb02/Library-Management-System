package librarysystem.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import librarysystem.model.Book;
import librarysystem.service.LibraryException;
import librarysystem.service.LibraryService;
import librarysystem.view.MainView;

import java.util.Optional;

/** Connects the JavaFX view to the library service. */
public class LibraryController {
    private final LibraryService service;
    private final MainView view;

    public LibraryController(LibraryService service, MainView view) {
        this.service = service;
        this.view = view;
        wireEvents();
        refresh();
    }

    private void wireEvents() {
        view.getAddButton().setOnAction(event -> addBook());
        view.getUpdateButton().setOnAction(event -> updateBook());
        view.getDeleteButton().setOnAction(event -> deleteBook());
        view.getStatusButton().setOnAction(event -> changeStatus());
        view.getClearButton().setOnAction(event -> view.clearForm());
        view.getTable().getSelectionModel().selectedItemProperty().addListener(
                (observable, oldBook, selectedBook) -> {
                    if (selectedBook != null) view.loadBook(selectedBook);
                });
    }

    private void addBook() {
        try {
            service.addBook(view.getIdField().getText(),
                    view.getTitleField().getText(),
                    view.getAuthorField().getText());
            refresh();
            view.clearForm();
            view.showAlert(Alert.AlertType.INFORMATION, "Book Added",
                    "The book was added successfully.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void updateBook() {
        Book selected = getSelectedBook();
        if (selected == null) return;
        try {
            service.updateBook(selected.getId(),
                    view.getTitleField().getText(),
                    view.getAuthorField().getText());
            refresh();
            view.loadBook(selected);
            view.showAlert(Alert.AlertType.INFORMATION, "Book Updated",
                    "The book details were updated successfully.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void deleteBook() {
        Book selected = getSelectedBook();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Book");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Delete book '" + selected.getTitle() +
                "' (ID " + selected.getId() + ")?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            service.deleteBook(selected.getId());
            view.clearForm();
            refresh();
            view.showAlert(Alert.AlertType.INFORMATION, "Book Deleted",
                    "The book was deleted successfully.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private void changeStatus() {
        Book selected = getSelectedBook();
        if (selected == null) return;
        try {
            service.updateBookStatus(selected.getId(), view.getStatusComboBox().getValue());
            refresh();
            view.loadBook(selected);
            view.showAlert(Alert.AlertType.INFORMATION, "Status Updated",
                    "The book status was changed successfully.");
        } catch (LibraryException exception) {
            showError(exception);
        }
    }

    private Book getSelectedBook() {
        Book selected = view.getTable().getSelectionModel().getSelectedItem();
        if (selected == null) {
            view.showAlert(Alert.AlertType.WARNING, "No Book Selected",
                    "Select a book from the table first.");
        }
        return selected;
    }

    private void refresh() {
        view.refreshTable(service.getAllBooks());
    }

    private void showError(LibraryException exception) {
        view.showAlert(Alert.AlertType.ERROR, "Operation Failed", exception.getMessage());
    }
}
