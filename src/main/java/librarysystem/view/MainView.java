package librarysystem.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import librarysystem.model.Book;
import librarysystem.model.BookStatus;

import java.util.List;

/** Builds and exposes the JavaFX library-management interface. */
public class MainView {
    private final BorderPane root = new BorderPane();
    private final TextField idField = new TextField();
    private final TextField titleField = new TextField();
    private final TextField authorField = new TextField();
    private final ComboBox<BookStatus> statusComboBox =
            new ComboBox<>(FXCollections.observableArrayList(BookStatus.values()));
    private final TableView<Book> table = new TableView<>();
    private final ObservableList<Book> tableData = FXCollections.observableArrayList();
    private final Button addButton = createButton("Add Book", "primary-button");
    private final Button updateButton = createButton("Update Book", "secondary-button");
    private final Button deleteButton = createButton("Delete Book", "danger-button");
    private final Button statusButton = createButton("Change Status", "status-button");
    private final Button clearButton = createButton("Clear Form", "clear-button");

    public MainView() {
        statusComboBox.setValue(BookStatus.AVAILABLE);
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        buildHeader();
        buildFormAndTable();
        table.setItems(tableData);
    }

    public BorderPane getRoot() { return root; }
    public TextField getIdField() { return idField; }
    public TextField getTitleField() { return titleField; }
    public TextField getAuthorField() { return authorField; }
    public ComboBox<BookStatus> getStatusComboBox() { return statusComboBox; }
    public TableView<Book> getTable() { return table; }
    public Button getAddButton() { return addButton; }
    public Button getUpdateButton() { return updateButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getStatusButton() { return statusButton; }
    public Button getClearButton() { return clearButton; }

    public void refreshTable(List<Book> books) {
        tableData.setAll(books);
        if (books.isEmpty()) {
            table.setPlaceholder(new Label("No books in the library. Add a book to get started."));
        }
    }

    public void loadBook(Book book) {
        idField.setText(book.getId());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        statusComboBox.setValue(book.getStatus());
        idField.setDisable(true);
    }

    public void clearForm() {
        idField.clear();
        titleField.clear();
        authorField.clear();
        statusComboBox.setValue(BookStatus.AVAILABLE);
        idField.setDisable(false);
        table.getSelectionModel().clearSelection();
    }

    public void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void buildHeader() {
        VBox header = new VBox(5);
        header.getStyleClass().add("header");
        Label title = new Label("Library Book Management");
        title.getStyleClass().add("header-title");
        Label subtitle = new Label("Manage books, availability, and status");
        subtitle.getStyleClass().add("header-subtitle");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);
    }

    private void buildFormAndTable() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content");
        content.setPadding(new Insets(22));

        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("card");
        Label formTitle = new Label("Book Details");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.getStyleClass().add("book-form");
        form.setHgap(14);
        form.setVgap(12);
        idField.setPromptText("Enter book ID");
        titleField.setPromptText("Enter title");
        authorField.setPromptText("Enter author");

        form.add(createField("Book ID", idField), 0, 0);
        form.add(createField("Title", titleField), 1, 0);
        form.add(createField("Author", authorField), 2, 0);
        VBox statusField = createField("Status", statusComboBox);
        form.add(statusField, 3, 0);
        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints constraints =
                    new javafx.scene.layout.ColumnConstraints();
            constraints.setPercentWidth(25);
            form.getColumnConstraints().add(constraints);
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().addAll(addButton, updateButton, deleteButton, statusButton,
                new Region(), clearButton);
        HBox.setHgrow(actions.getChildren().get(actions.getChildren().size() - 2),
                Priority.ALWAYS);

        formCard.getChildren().addAll(formTitle, form, actions);

        VBox tableCard = new VBox(10);
        tableCard.getStyleClass().add("card");
        HBox tableHeader = new HBox();
        Label tableTitle = new Label("Library Books");
        tableTitle.getStyleClass().add("section-title");
        tableHeader.getChildren().add(tableTitle);

        configureTable();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        VBox.setVgrow(table, Priority.ALWAYS);
        tableCard.getChildren().addAll(tableHeader, table);

        content.getChildren().addAll(formCard, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        root.setCenter(scrollPane);
    }

    private VBox createField(String labelText, javafx.scene.control.Control control) {
        VBox field = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(control, Priority.ALWAYS);
        field.getChildren().addAll(label, control);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private void configureTable() {
        TableColumn<Book, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> data.getValue().idProperty());

        TableColumn<Book, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data -> data.getValue().titleProperty());

        TableColumn<Book, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(data -> data.getValue().authorProperty());

        TableColumn<Book, BookStatus> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        table.getColumns().setAll(idColumn, titleColumn, authorColumn, statusColumn);
        table.setPlaceholder(new Label("No books in the library."));
    }

    private Button createButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("action-button", styleClass);
        button.setTooltip(new Tooltip(text));
        return button;
    }
}
