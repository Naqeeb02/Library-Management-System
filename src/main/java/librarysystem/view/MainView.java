package librarysystem.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.util.Callback;
import librarysystem.LibraryBranding;
import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import librarysystem.model.LibraryActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Builds the institution-branded dashboard and book-management screens. */
public class MainView {
    private enum Page {
        DASHBOARD("Dashboard", "Overview"),
        BOOKS("Books Management", "Library / Books"),
        ISSUED("Issued Books", "Library / Issued"),
        RESERVED("Reserved Books", "Library / Reserved"),
        SETTINGS("Settings", "Application / Settings"),
        ABOUT("About Application", "Application / About");

        private final String title;
        private final String breadcrumb;

        Page(String title, String breadcrumb) {
            this.title = title;
            this.breadcrumb = breadcrumb;
        }
    }

    /** A requested operation selected from a book details dialog. */
    public enum BookAction {
        VIEW_DETAILS,
        EDIT,
        CHANGE_STATUS,
        DELETE,
        RETURN_BOOK
    }

    /** Values entered in the add/edit book dialog. */
    public record BookFormData(String id, String title, String author) {}

    private final LibraryBranding branding = LibraryBranding.load();
    private final BorderPane root = new BorderPane();
    private final BorderPane workspace = new BorderPane();
    private final StackPane pageHost = new StackPane();
    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final FilteredList<Book> filteredBooks = new FilteredList<>(books);
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList("All books", "Available", "Issued", "Reserved"));
    private final ComboBox<String> densitySetting = new ComboBox<>(
            FXCollections.observableArrayList("Comfortable", "Compact"));
    private final TableView<Book> managementTable = new TableView<>();
    private final TableView<Book> recentTable = new TableView<>();
    private final Label totalBooksValue = new Label("0");
    private final Label availableBooksValue = new Label("0");
    private final Label issuedBooksValue = new Label("0");
    private final Label reservedBooksValue = new Label("0");
    private final Label resultCount = new Label("0 books");
    private final Label pageTitle = new Label();
    private final Label bookPageHeading = new Label();
    private final Label bookPageSubtitle = new Label();
    private final Label breadcrumb = new Label();
    private final VBox activityList = new VBox(10);
    private final VBox dashboardPage = new VBox();
    private final VBox booksPage = new VBox();
    private final VBox settingsPage = new VBox();
    private final VBox aboutPage = new VBox();
    private ScrollPane dashboardScroll;
    private ScrollPane booksScroll;
    private ScrollPane settingsScroll;
    private ScrollPane aboutScroll;
    private final Button addBookButton = button("+  Add new book", "button-primary");
    private Page currentPage = Page.DASHBOARD;
    private Consumer<Book> editHandler = book -> {};
    private Consumer<Book> deleteHandler = book -> {};
    private BiConsumer<Book, BookAction> detailsHandler = (book, action) -> {};
    private BiConsumer<Book, BookStatus> statusHandler = (book, status) -> {};
    private Runnable addHandler = () -> {};

    public MainView() {
        configureRoot();
        buildDashboard();
        buildBooksPage();
        buildSettingsPage();
        buildAboutPage();
        setBooks(List.of());
        setActivities(List.of());
        showPage(Page.DASHBOARD);
    }

    /** Returns the root node for the application's JavaFX scene. */
    public BorderPane getRoot() {
        return root;
    }

    /** Returns the configured library name for the application window. */
    public String getLibraryName() {
        return branding.getLibraryName();
    }

    /** Returns the configured application title. */
    public String getApplicationName() {
        return branding.getApplicationName();
    }

    /** Creates a window icon from the supplied logo or the neutral ACL project emblem. */
    public Image createWindowIcon() {
        Canvas canvas = new Canvas(64, 64);
        var logoUrl = getClass().getResource(branding.getLogoResource());
        if (logoUrl == null) {
            canvas = createPlaceholderLogo(64);
        } else {
            Image logo = new Image(logoUrl.toExternalForm(), false);
            if (logo.isError()) {
                throw new IllegalStateException("Could not load the configured library logo.",
                        logo.getException());
            }
            double scale = Math.min(64 / logo.getWidth(), 64 / logo.getHeight());
            double width = logo.getWidth() * scale;
            double height = logo.getHeight() * scale;
            canvas.getGraphicsContext2D().drawImage(logo,
                    (64 - width) / 2, (64 - height) / 2, width, height);
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, new WritableImage(64, 64));
    }

    /** Sets the action used when the user requests to add a book. */
    public void setAddHandler(Runnable handler) {
        addHandler = handler;
    }

    /** Sets the action used when the user requests to edit a book. */
    public void setEditHandler(Consumer<Book> handler) {
        editHandler = handler;
    }

    /** Sets the action used when the user requests to delete a book. */
    public void setDeleteHandler(Consumer<Book> handler) {
        deleteHandler = handler;
    }

    /** Sets the action used when the user requests book details. */
    public void setDetailsHandler(BiConsumer<Book, BookAction> handler) {
        detailsHandler = handler;
    }

    /** Sets the action used when the user requests a status change. */
    public void setStatusHandler(BiConsumer<Book, BookStatus> handler) {
        statusHandler = handler;
    }

    /** Replaces displayed data and recalculates dashboard statistics. */
    public void setBooks(List<Book> currentBooks) {
        books.setAll(currentBooks);
        updateDashboardStats(currentBooks);
        updateRecentBooks(currentBooks);
        filterBooks();
    }

    /** Refreshes the recent activity feed from completed service operations. */
    public void setActivities(List<LibraryActivity> activities) {
        activityList.getChildren().clear();
        if (activities.isEmpty()) {
            Label empty = new Label("No activity yet. Completed book actions will appear here.");
            empty.getStyleClass().add("empty-activity");
            activityList.getChildren().add(empty);
            return;
        }
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");
        activities.stream().limit(8).forEach(activity ->
                activityList.getChildren().add(activityRow(activity, timeFormat)));
    }

    /** Opens the add/edit dialog and returns form data to the supplied handler. */
    public void showBookDialog(Book selectedBook, Consumer<BookFormData> onSave) {
        Dialog<BookFormData> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(selectedBook == null ? "Add new book" : "Edit book");
        dialog.getDialogPane().getStyleClass().add("book-dialog");

        TextField id = new TextField(selectedBook == null ? "" : selectedBook.getId());
        id.setPromptText("e.g. ACL-1042");
        id.setDisable(selectedBook != null);
        id.setPrefHeight(42);
        TextField title = new TextField(selectedBook == null ? "" : selectedBook.getTitle());
        title.setPromptText("Enter book title");
        title.setPrefHeight(42);
        TextField author = new TextField(selectedBook == null ? "" : selectedBook.getAuthor());
        author.setPromptText("Enter author name");
        author.setPrefHeight(42);
        Label validation = new Label();
        validation.getStyleClass().add("validation-message");

        GridPane fields = new GridPane();
        fields.setVgap(16);
        fields.setHgap(16);
        fields.add(field("BOOK ID", id), 0, 0);
        fields.add(field("BOOK TITLE", title), 0, 1);
        fields.add(field("AUTHOR", author), 0, 2);
        if (selectedBook == null) {
            Label statusNote = new Label("New books are added as AVAILABLE.");
            statusNote.getStyleClass().add("dialog-hint");
            fields.add(statusNote, 0, 3);
        }
        fields.add(validation, 0, 4);
        fields.setPadding(new Insets(0, 0, 0, 0));

        VBox dialogContent = new VBox(18);
        dialogContent.getStyleClass().add("book-dialog-content");
        Label eyebrow = new Label(selectedBook == null
                ? "CATALOGUE ENTRY" : "CATALOGUE RECORD");
        eyebrow.getStyleClass().add("dialog-eyebrow");
        Label intro = new Label(selectedBook == null
                ? "Add a title to the Army Central Library collection."
                : "Update the details for this library book.");
        intro.getStyleClass().add("dialog-intro");
        intro.setWrapText(true);
        intro.setMaxWidth(420);
        VBox heading = new VBox(6, eyebrow, intro);
        VBox formCard = new VBox(16, fields);
        formCard.getStyleClass().add("dialog-form-card");
        dialogContent.getChildren().addAll(heading, formCard);
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setMinWidth(460);

        ButtonType saveType = new ButtonType(
                selectedBook == null ? "Save book" : "Save changes",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("button-primary");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass()
                .add("button-dialog-cancel");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (id.getText().isBlank() || title.getText().isBlank() || author.getText().isBlank()) {
                validation.setText("Complete all required fields before saving.");
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == saveType
                ? new BookFormData(id.getText().trim(), title.getText().trim(),
                        author.getText().trim())
                : null);
        dialog.setOnShown(event -> (selectedBook == null ? id : title).requestFocus());
        dialog.showAndWait().ifPresent(onSave);
    }

    /** Displays book details and offers the available record operations. */
    public void showBookDetails(Book book, long fineToDate) {
        Dialog<BookAction> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Book details");
        dialog.setHeaderText(book.getTitle());

        VBox details = new VBox(12);
        details.setPadding(new Insets(10, 0, 8, 0));
        details.getChildren().addAll(
                detailLine("Book ID", book.getId()),
                detailLine("Title", book.getTitle()),
                detailLine("Author", book.getAuthor()),
                detailLine("Status", book.getStatus().name()));
        if (book.getIssueDate() != null) {
            details.getChildren().addAll(
                    detailLine("Issue date", book.getIssueDate().toString()),
                    detailLine("Return-by date", book.getDueDate().toString()));
            if (book.getStatus() == BookStatus.ISSUED) {
                details.getChildren().add(detailLine("Overdue fine to date",
                        "PKR " + fineToDate));
            } else if (book.getReturnedDate() != null) {
                details.getChildren().add(detailLine("Last returned", book.getReturnedDate().toString()));
            }
        }
        dialog.getDialogPane().setContent(details);

        ButtonType edit = new ButtonType("Edit", ButtonBar.ButtonData.LEFT);
        ButtonType changeStatus = new ButtonType(
                book.getStatus() == BookStatus.ISSUED ? "Return Book" : "Change status",
                ButtonBar.ButtonData.LEFT);
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(edit, changeStatus, delete, ButtonType.CLOSE);
        dialog.setResultConverter(button -> {
            if (button == edit) return BookAction.EDIT;
            if (button == changeStatus) {
                return book.getStatus() == BookStatus.ISSUED
                        ? BookAction.RETURN_BOOK : BookAction.CHANGE_STATUS;
            }
            if (button == delete) return BookAction.DELETE;
            return null;
        });
        dialog.showAndWait().ifPresent(action -> detailsHandler.accept(book, action));
    }

    /** Opens the required return-by date picker before an issue operation. */
    public void showIssueDialog(Book book, Consumer<LocalDate> onIssue) {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Issue book");
        dialog.getDialogPane().getStyleClass().add("book-dialog");

        DatePicker dueDate = new DatePicker(LocalDate.now());
        dueDate.setPromptText("Select return-by date");
        dueDate.setPrefHeight(42);
        dueDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        Label validation = new Label();
        validation.getStyleClass().add("validation-message");
        VBox content = new VBox(14,
                detailLine("Book", book.getTitle() + " · " + book.getId()),
                field("RETURN-BY DATE", dueDate),
                new Label("Fine after the due date: PKR "
                        + librarysystem.service.LibraryService.DAILY_OVERDUE_FINE_PKR
                        + " per day."),
                validation);
        content.getStyleClass().add("book-dialog-content");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(470);

        ButtonType issue = new ButtonType("Issue book", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issue, ButtonType.CANCEL);
        Node issueButton = dialog.getDialogPane().lookupButton(issue);
        issueButton.getStyleClass().add("button-primary");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass()
                .add("button-dialog-cancel");
        issueButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (dueDate.getValue() == null || dueDate.getValue().isBefore(LocalDate.now())) {
                validation.setText("Choose a return-by date that is today or later.");
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == issue ? dueDate.getValue() : null);
        dialog.showAndWait().ifPresent(onIssue);
    }

    /** Opens a focused status picker for the selected book. */
    public void showStatusDialog(Book book, Consumer<BookStatus> onSave) {
        Dialog<BookStatus> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Change book status");
        dialog.setHeaderText("Choose a status for " + book.getTitle());
        ComboBox<BookStatus> status = new ComboBox<>(
                FXCollections.observableArrayList(BookStatus.values()));
        status.setValue(book.getStatus());
        status.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(field("Status", status));
        ButtonType save = new ButtonType("Save status", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == save ? status.getValue() : null);
        dialog.showAndWait().ifPresent(onSave);
    }

    /** Shows a confirmation prompt and returns true only for an affirmative response. */
    public boolean confirmDelete(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Delete book");
        alert.setHeaderText("Delete \"" + book.getTitle() + "\"?");
        alert.setContentText("This will permanently remove book " + book.getId() + ".");
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    /** Confirms a return operation before changing an issued book to available. */
    public boolean confirmReturn(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Return book");
        alert.setHeaderText("Record the return of \"" + book.getTitle() + "\"?");
        alert.setContentText("Return-by date: " + book.getDueDate());
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    /** Displays a short informational, warning, or error notification. */
    public void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureRoot() {
        root.getStyleClass().add("app-root");
        root.setLeft(createSidebar());
        root.setCenter(workspace);
        workspace.setTop(createToolbar());
        workspace.setCenter(pageHost);
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(246);
        sidebar.setMinWidth(218);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");
        brand.getChildren().addAll(createBrandLogo(42), new VBox(3,
                brandLabel(branding.getLibraryName(), "brand-name"),
                brandLabel(branding.getInstitutionName(), "brand-caption")));

        Label menuLabel = new Label("LIBRARY");
        menuLabel.getStyleClass().add("nav-caption");
        VBox navigation = new VBox(5);
        navigation.getStyleClass().add("navigation");
        Button dashboard = navButton("▦", "Dashboard", Page.DASHBOARD);
        Button allBooks = navButton("▤", "Books Management", Page.BOOKS);
        Button addBook = navButton("+", "Add New Book", null);
        Button issued = navButton("↗", "Issued Books", Page.ISSUED);
        Button reserved = navButton("▱", "Reserved Books", Page.RESERVED);
        navigation.getChildren().addAll(dashboard, allBooks, addBook, issued, reserved);
        addBook.setOnAction(event -> addHandler.run());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label systemLabel = new Label("APPLICATION");
        systemLabel.getStyleClass().add("nav-caption");
        VBox applicationNavigation = new VBox(5);
        applicationNavigation.getStyleClass().add("navigation");
        Button settings = navButton("⚙", "Settings", Page.SETTINGS);
        Button about = navButton("ⓘ", "About Application", Page.ABOUT);
        applicationNavigation.getChildren().addAll(settings, about);

        HBox profile = new HBox(10);
        profile.setAlignment(Pos.CENTER_LEFT);
        profile.getStyleClass().add("profile");
        Label avatar = new Label("L");
        avatar.getStyleClass().add("profile-avatar");
        profile.getChildren().addAll(avatar, new VBox(2,
                brandLabel("Librarian", "profile-name"),
                brandLabel("Library administrator", "profile-role")));

        sidebar.getChildren().addAll(brand, menuLabel, navigation, spacer, systemLabel,
                applicationNavigation, new Separator(), profile);
        return sidebar;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(18);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        VBox titles = new VBox(3);
        pageTitle.getStyleClass().add("toolbar-title");
        breadcrumb.getStyleClass().add("breadcrumb");
        titles.getChildren().addAll(pageTitle, breadcrumb);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        searchField.setPromptText("Search by book ID, title, or author");
        searchField.getStyleClass().add("global-search");
        searchField.setPrefWidth(220);
        searchField.setMinWidth(170);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterBooks());
        Label date = new Label(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)));
        date.getStyleClass().add("toolbar-date");
        Label profile = new Label("L");
        profile.getStyleClass().add("toolbar-avatar");
        toolbar.getChildren().addAll(titles, spacer, searchField, date, profile);
        return toolbar;
    }

    private void buildDashboard() {
        dashboardPage.setSpacing(22);
        dashboardPage.getStyleClass().add("page-content");
        dashboardPage.setPadding(new Insets(28, 32, 32, 32));

        HBox welcome = new HBox(18);
        welcome.setAlignment(Pos.CENTER_LEFT);
        VBox welcomeText = new VBox(7);
        Label title = new Label("Welcome to " + toTitleCase(branding.getLibraryName()));
        title.getStyleClass().add("page-heading");
        Label subtitle = new Label("Manage your collection and monitor book activity.");
        subtitle.getStyleClass().add("page-subtitle");
        welcomeText.getChildren().addAll(title, subtitle);
        Region welcomeSpacer = new Region();
        HBox.setHgrow(welcomeSpacer, Priority.ALWAYS);
        addBookButton.setOnAction(event -> addHandler.run());
        welcome.getChildren().addAll(welcomeText, welcomeSpacer, addBookButton);

        TilePane stats = new TilePane();
        stats.setHgap(16);
        stats.setVgap(16);
        stats.setPrefColumns(4);
        stats.setPrefTileWidth(250);
        stats.getStyleClass().add("stats-grid");
        stats.widthProperty().addListener((observable, oldWidth, newWidth) ->
                stats.setPrefColumns(newWidth.doubleValue() < 850 ? 2 : 4));
        stats.getChildren().addAll(
                statCard("Total books", totalBooksValue, "In your collection", "stat-navy", "▤"),
                statCard("Available", availableBooksValue, "Ready to be borrowed", "stat-green", "✓"),
                statCard("Issued", issuedBooksValue, "Currently checked out", "stat-amber", "↗"),
                statCard("Reserved", reservedBooksValue, "Waiting for pickup", "stat-blue", "▱"));

        VBox recentCard = new VBox(14);
        recentCard.getStyleClass().add("surface-card");
        HBox recentHeader = new HBox();
        recentHeader.setAlignment(Pos.CENTER_LEFT);
        VBox recentTitles = new VBox(4);
        Label recentTitle = new Label("Recently added books");
        recentTitle.getStyleClass().add("section-heading");
        Label recentSubtitle = new Label("The latest additions to your collection");
        recentSubtitle.getStyleClass().add("muted-text");
        recentTitles.getChildren().addAll(recentTitle, recentSubtitle);
        Region recentSpacer = new Region();
        HBox.setHgrow(recentSpacer, Priority.ALWAYS);
        Button viewAll = button("View all  →", "button-quiet");
        viewAll.setOnAction(event -> showPage(Page.BOOKS));
        recentHeader.getChildren().addAll(recentTitles, recentSpacer, viewAll);
        configureTable(recentTable, false);
        recentTable.setPrefHeight(270);
        VBox.setVgrow(recentTable, Priority.ALWAYS);
        recentCard.getChildren().addAll(recentHeader, recentTable);

        VBox activityCard = new VBox(14);
        activityCard.getStyleClass().add("surface-card");
        Label activityTitle = new Label("Recent activity");
        activityTitle.getStyleClass().add("section-heading");
        activityCard.getChildren().addAll(activityTitle, activityList);

        VBox dashboardLower = new VBox(18, recentCard, activityCard);
        dashboardLower.setAlignment(Pos.TOP_LEFT);
        recentCard.setMinWidth(0);
        recentCard.setMaxWidth(Double.MAX_VALUE);
        activityCard.setMaxWidth(Double.MAX_VALUE);

        VBox quickActions = new VBox(12);
        quickActions.getStyleClass().add("surface-card");
        Label quickTitle = new Label("Quick actions");
        quickTitle.getStyleClass().add("section-heading");
        HBox actionRow = new HBox(10);
        Button quickAdd = button("+  Add new book", "button-secondary");
        quickAdd.setOnAction(event -> addHandler.run());
        Button quickView = button("▤  View all books", "button-quiet");
        quickView.setOnAction(event -> showPage(Page.BOOKS));
        Button quickIssued = button("↗  Manage issued books", "button-quiet");
        quickIssued.setOnAction(event -> showPage(Page.ISSUED));
        actionRow.getChildren().addAll(quickAdd, quickView, quickIssued);
        quickActions.getChildren().addAll(quickTitle, actionRow);

        dashboardPage.getChildren().addAll(welcome, stats, dashboardLower, quickActions);
        dashboardScroll = addScrollablePage(dashboardPage);
    }

    private void buildBooksPage() {
        booksPage.setSpacing(18);
        booksPage.getStyleClass().add("page-content");
        booksPage.setPadding(new Insets(28, 32, 32, 32));
        HBox heading = new HBox(16);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(6);
        bookPageHeading.getStyleClass().add("page-heading");
        bookPageSubtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(bookPageHeading, bookPageSubtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        resultCount.getStyleClass().add("count-label");
        Button pageAdd = button("+  Add new book", "button-primary");
        pageAdd.setOnAction(event -> addHandler.run());
        heading.getChildren().addAll(titleBox, spacer, resultCount, pageAdd);

        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filter-bar");
        Label filterLabel = new Label("Status");
        filterLabel.getStyleClass().add("field-label");
        statusFilter.setValue("All books");
        statusFilter.setPrefWidth(165);
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterBooks());
        filters.getChildren().addAll(filterLabel, statusFilter);

        VBox tableCard = new VBox(0);
        tableCard.getStyleClass().add("table-card");
        configureTable(managementTable, true);
        managementTable.setItems(filteredBooks);
        VBox.setVgrow(managementTable, Priority.ALWAYS);
        tableCard.getChildren().add(managementTable);

        booksPage.getChildren().addAll(heading, filters, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        booksScroll = addScrollablePage(booksPage);
    }

    private void buildSettingsPage() {
        settingsPage.setSpacing(18);
        settingsPage.getStyleClass().add("page-content");
        settingsPage.setPadding(new Insets(28, 32, 32, 32));
        settingsPage.getChildren().addAll(pageHeading("Settings",
                "Adjust the application interface and book-management preferences."));

        VBox appearance = new VBox(15);
        appearance.getStyleClass().add("surface-card");
        appearance.getChildren().addAll(sectionTitle("Appearance"),
                new Label("Choose how book rows are displayed in tables."));
        densitySetting.setValue("Comfortable");
        densitySetting.setPrefWidth(190);
        densitySetting.valueProperty().addListener((observable, oldValue, newValue) ->
                setCompactRows("Compact".equals(newValue)));
        appearance.getChildren().add(field("Table row density", densitySetting));

        VBox operations = new VBox(14);
        operations.getStyleClass().add("surface-card");
        operations.getChildren().add(sectionTitle("Book operations"));
        Label confirmationNote = new Label("A confirmation is always required before a book is deleted.");
        confirmationNote.getStyleClass().add("muted-text");
        operations.getChildren().add(confirmationNote);
        Button resetFilters = button("Reset search and filters", "button-secondary");
        resetFilters.setOnAction(event -> {
            searchField.clear();
            statusFilter.setValue("All books");
            showMessage(Alert.AlertType.INFORMATION, "Preferences reset",
                    "Search and status filters have been reset.");
        });
        operations.getChildren().add(resetFilters);

        VBox identity = new VBox(10);
        identity.getStyleClass().add("surface-card");
        Label profileDescription = new Label(
                "A library workspace for organizing the book collection, checking availability, "
                        + "and keeping issue, return, due-date, and reservation records clear.");
        profileDescription.getStyleClass().add("page-subtitle");
        profileDescription.setWrapText(true);
        Label projectNote = new Label(
                "Academic project. The ACL emblem is a neutral project mark, not an official insignia.");
        projectNote.getStyleClass().add("detail-value");
        projectNote.setWrapText(true);
        identity.getChildren().addAll(sectionTitle("Library profile"),
                new Label("Library: " + branding.getLibraryName()),
                new Label("Location: " + branding.getInstitutionName()),
                profileDescription,
                projectNote);
        settingsPage.getChildren().addAll(appearance, operations, identity);
        settingsScroll = addScrollablePage(settingsPage);
    }

    private void buildAboutPage() {
        aboutPage.setSpacing(18);
        aboutPage.getStyleClass().add("page-content");
        aboutPage.setPadding(new Insets(28, 32, 32, 32));
        aboutPage.getChildren().add(pageHeading("About Application",
                "Library system information and institutional identity."));

        VBox aboutCard = new VBox(15);
        aboutCard.getStyleClass().add("surface-card");
        HBox identity = new HBox(16);
        identity.setAlignment(Pos.CENTER_LEFT);
        VBox identityText = new VBox(5,
                brandLabel(toTitleCase(branding.getLibraryName()), "about-library"),
                brandLabel(toTitleCase(branding.getInstitutionName()), "about-institution"));
        identity.getChildren().addAll(createBrandLogo(58), identityText);
        aboutCard.getChildren().addAll(identity, new Separator(),
                detailLine("Application", branding.getApplicationName()),
                detailLine("Subtitle", branding.getSubtitle()),
                detailLine("Project context",
                        "Academic Software Engineering Project; not an official or deployed Army application"),
                detailLine("Version", "1.0.0"),
                detailLine("Technologies", "Java · JavaFX · Maven"),
                detailLine("Storage", "In-memory collection; data resets when the app closes"),
                detailLine("Project emblem", logoAvailable()
                        ? "Provided logo resource; replace only with an authorized file"
                        : "Temporary ACL project emblem — not an official insignia"));
        aboutPage.getChildren().add(aboutCard);
        aboutScroll = addScrollablePage(aboutPage);
    }

    private ScrollPane addScrollablePage(Node page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        pageHost.getChildren().add(scroll);
        return scroll;
    }

    private VBox statCard(String title, Label value, String caption, String accent, String iconText) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("stat-card", accent);
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(title);
        name.getStyleClass().add("stat-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label icon = new Label(iconText);
        icon.getStyleClass().add("stat-icon");
        top.getChildren().addAll(name, spacer, icon);
        value.getStyleClass().add("stat-value");
        Label description = new Label(caption);
        description.getStyleClass().add("stat-caption");
        card.getChildren().addAll(top, value, description);
        return card;
    }

    private void configureTable(TableView<Book> table, boolean withActions) {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No books in the library yet."));

        TableColumn<Book, String> id = new TableColumn<>("BOOK ID");
        id.setCellValueFactory(data -> data.getValue().idProperty());
        id.setMinWidth(70);
        id.setMaxWidth(110);

        TableColumn<Book, String> title = new TableColumn<>("BOOK TITLE");
        title.setCellValueFactory(data -> data.getValue().titleProperty());
        title.setMinWidth(120);

        TableColumn<Book, String> author = new TableColumn<>("AUTHOR");
        author.setCellValueFactory(data -> data.getValue().authorProperty());
        author.setMinWidth(100);

        TableColumn<Book, LocalDate> dueDate = new TableColumn<>("RETURN BY");
        dueDate.setCellValueFactory(data -> data.getValue().dueDateProperty());
        dueDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().remove("overdue-date");
                } else if (date == null) {
                    setText("—");
                    getStyleClass().remove("overdue-date");
                } else {
                    setText(date.toString());
                    Book book = getTableRow() == null ? null : getTableRow().getItem();
                    boolean overdue = book != null
                            && book.getStatus() == BookStatus.ISSUED
                            && LocalDate.now().isAfter(date);
                    if (overdue && !getStyleClass().contains("overdue-date")) {
                        getStyleClass().add("overdue-date");
                    } else if (!overdue) {
                        getStyleClass().remove("overdue-date");
                    }
                }
            }
        });
        dueDate.setMinWidth(95);
        dueDate.setMaxWidth(125);

        TableColumn<Book, BookStatus> status = new TableColumn<>("STATUS");
        status.setCellValueFactory(data -> data.getValue().statusProperty());
        status.setCellFactory(column -> new StatusCell());
        status.setMinWidth(82);
        status.setMaxWidth(120);

        if (withActions) {
            TableColumn<Book, Void> actions = new TableColumn<>("ACTIONS");
            actions.setCellFactory(actionCellFactory());
            actions.setMinWidth(185);
            actions.setMaxWidth(205);
            table.getColumns().setAll(List.of(id, title, author, dueDate, status, actions));
        } else {
            table.getColumns().setAll(List.of(id, title, author, dueDate, status));
        }
    }

    private Callback<TableColumn<Book, Void>, TableCell<Book, Void>> actionCellFactory() {
        return column -> new TableCell<>() {
            private final Button view = button("View", "row-view");
            private final Button edit = button("Edit", "row-edit");
            private final Button status = button("Status", "row-status");
            private final Button returnBook = button("Return", "row-status");
            private final Button delete = button("Delete", "row-delete");
            private final HBox actions = new HBox(3, view, edit, status, returnBook, delete);

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                view.setOnAction(event ->
                        detailsHandler.accept(selectedBook(), BookAction.VIEW_DETAILS));
                edit.setOnAction(event -> editHandler.accept(selectedBook()));
                status.setOnAction(event -> {
                    Book book = selectedBook();
                    showStatusDialog(book, newStatus -> statusHandler.accept(book, newStatus));
                });
                returnBook.setOnAction(event ->
                        detailsHandler.accept(selectedBook(), BookAction.RETURN_BOOK));
                delete.setOnAction(event -> deleteHandler.accept(selectedBook()));
            }

            private Book selectedBook() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                boolean issued = !empty && selectedBook().getStatus() == BookStatus.ISSUED;
                status.setVisible(!issued);
                status.setManaged(!issued);
                returnBook.setVisible(issued);
                returnBook.setManaged(issued);
                setGraphic(empty ? null : actions);
            }
        };
    }

    private static final class StatusCell extends TableCell<Book, BookStatus> {
        @Override
        protected void updateItem(BookStatus value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label badge = new Label(value.name());
            badge.getStyleClass().addAll("status-badge",
                    "status-" + value.name().toLowerCase(Locale.ROOT));
            setText(null);
            setGraphic(badge);
        }
    }

    private HBox activityRow(LibraryActivity activity, DateTimeFormatter timeFormat) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label marker = new Label("•");
        marker.getStyleClass().add("activity-marker");
        VBox description = new VBox(3,
                new Label(activity.action() + " “" + activity.bookTitle() + "”"),
                new Label("ID " + activity.bookId() + " · "
                        + activity.occurredAt().format(timeFormat)));
        description.getChildren().get(0).getStyleClass().add("activity-title");
        description.getChildren().get(1).getStyleClass().add("activity-time");
        row.getChildren().addAll(marker, description);
        return row;
    }

    private VBox detailLine(String label, String value) {
        VBox line = new VBox(4);
        Label name = new Label(label);
        name.getStyleClass().add("detail-label");
        Label content = new Label(value);
        content.getStyleClass().add("detail-value");
        content.setWrapText(true);
        line.getChildren().addAll(name, content);
        return line;
    }

    private VBox pageHeading(String title, String subtitle) {
        VBox heading = new VBox(6);
        Label headingLabel = new Label(title);
        headingLabel.getStyleClass().add("page-heading");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        heading.getChildren().addAll(headingLabel, subtitleLabel);
        return heading;
    }

    private Label sectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("section-heading");
        return title;
    }

    private Button navButton(String icon, String text, Page page) {
        Button button = new Button(icon + "    " + text);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        if (page != null) {
            button.setOnAction(event -> showPage(page));
            button.setUserData(page);
        }
        return button;
    }

    private Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("app-button", styleClass);
        button.setTooltip(new Tooltip(text));
        return button;
    }

    private VBox field(String labelText, Node control) {
        VBox wrapper = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        wrapper.getChildren().addAll(label, control);
        return wrapper;
    }

    private Label brandLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private Node createBrandLogo(double size) {
        var logoUrl = getClass().getResource(branding.getLogoResource());
        if (logoUrl == null) {
            return createPlaceholderLogo(size);
        }
        Image logo = new Image(logoUrl.toExternalForm(), false);
        if (logo.isError()) {
            throw new IllegalStateException("Could not load the configured library logo.",
                    logo.getException());
        }
        ImageView image = new ImageView(logo);
        image.setPreserveRatio(true);
        image.setFitWidth(size);
        image.setFitHeight(size);
        image.getStyleClass().add("brand-image");
        return image;
    }

    private Canvas createPlaceholderLogo(double size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double scale = size / 64;
        graphics.setFill(Color.web("#14243A"));
        graphics.fillRoundRect(0, 0, size, size, size * 0.24, size * 0.24);
        graphics.setFill(Color.web("#FFFFFF"));
        graphics.fillRoundRect(10 * scale, 10 * scale, 20 * scale, 31 * scale,
                3 * scale, 3 * scale);
        graphics.fillRoundRect(34 * scale, 10 * scale, 20 * scale, 31 * scale,
                3 * scale, 3 * scale);
        graphics.setStroke(Color.web("#C5A15B"));
        graphics.setLineWidth(Math.max(1.2, 2 * scale));
        graphics.strokeLine(32 * scale, 14 * scale, 32 * scale, 39 * scale);
        graphics.strokeLine(14 * scale, 17 * scale, 26 * scale, 17 * scale);
        graphics.strokeLine(14 * scale, 22 * scale, 26 * scale, 22 * scale);
        graphics.strokeLine(38 * scale, 17 * scale, 50 * scale, 17 * scale);
        graphics.strokeLine(38 * scale, 22 * scale, 50 * scale, 22 * scale);
        graphics.setFill(Color.web("#E6D1A5"));
        graphics.setFont(javafx.scene.text.Font.font("Segoe UI", 9 * scale));
        graphics.fillText(branding.getShortName(), 10 * scale, 55 * scale);
        return canvas;
    }

    private boolean logoAvailable() {
        return getClass().getResource(branding.getLogoResource()) != null;
    }

    private String toTitleCase(String value) {
        return Pattern.compile("\\b\\p{L}").matcher(value.toLowerCase(Locale.ROOT)).replaceAll(match ->
                match.group().toUpperCase(Locale.ROOT));
    }

    private void showPage(Page page) {
        currentPage = page;
        pageTitle.setText(page.title);
        breadcrumb.setText(page.breadcrumb);
        if (page == Page.ISSUED) {
            bookPageHeading.setText("Issued Books");
            bookPageSubtitle.setText("Review and manage books currently marked as issued.");
        } else if (page == Page.RESERVED) {
            bookPageHeading.setText("Reserved Books");
            bookPageSubtitle.setText("Review and manage books currently marked as reserved.");
        } else {
            bookPageHeading.setText("Books Management");
            bookPageSubtitle.setText("Search, update, and organize your library collection.");
        }
        Node target = switch (page) {
            case DASHBOARD -> dashboardScroll;
            case BOOKS, ISSUED, RESERVED -> booksScroll;
            case SETTINGS -> settingsScroll;
            case ABOUT -> aboutScroll;
        };
        for (Node node : pageHost.getChildren()) {
            boolean visible = node == target;
            node.setVisible(visible);
            node.setManaged(visible);
        }
        if (page == Page.ISSUED) {
            statusFilter.setValue("Issued");
        } else if (page == Page.RESERVED) {
            statusFilter.setValue("Reserved");
        } else if (page == Page.BOOKS) {
            statusFilter.setValue("All books");
        }
        updateNavigationSelection();
    }

    private void updateNavigationSelection() {
        VBox sidebar = (VBox) root.getLeft();
        VBox navigation = (VBox) sidebar.getChildren().get(2);
        for (Node node : navigation.getChildren()) {
            if (node instanceof Button button) {
                button.getStyleClass().remove("nav-item-active");
                if (button.getUserData() == currentPage) {
                    button.getStyleClass().add("nav-item-active");
                }
            }
        }
        for (Node node : sidebar.getChildren()) {
            if (node instanceof VBox group) {
                for (Node item : group.getChildren()) {
                    if (item instanceof Button button) {
                        button.getStyleClass().remove("nav-item-active");
                        if (button.getUserData() == currentPage) {
                            button.getStyleClass().add("nav-item-active");
                        }
                    }
                }
            }
        }
    }

    private void filterBooks() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String filter = statusFilter.getValue();
        filteredBooks.setPredicate(book -> {
            boolean matchesQuery = query.isEmpty()
                    || book.getId().toLowerCase(Locale.ROOT).contains(query)
                    || book.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    || book.getAuthor().toLowerCase(Locale.ROOT).contains(query);
            boolean matchesStatus = filter == null || filter.equals("All books")
                    || book.getStatus().name().equalsIgnoreCase(filter);
            return matchesQuery && matchesStatus;
        });
        resultCount.setText(filteredBooks.size() + (filteredBooks.size() == 1 ? " book" : " books"));
        if (books.isEmpty()) {
            managementTable.setPlaceholder(new Label("No books yet. Use Add New Book to get started."));
        } else if (filteredBooks.isEmpty()) {
            managementTable.setPlaceholder(new Label("No books match your search and status filter."));
        }
    }

    private void updateDashboardStats(List<Book> currentBooks) {
        totalBooksValue.setText(Integer.toString(currentBooks.size()));
        availableBooksValue.setText(Long.toString(countStatus(currentBooks, BookStatus.AVAILABLE)));
        issuedBooksValue.setText(Long.toString(countStatus(currentBooks, BookStatus.ISSUED)));
        reservedBooksValue.setText(Long.toString(countStatus(currentBooks, BookStatus.RESERVED)));
    }

    private long countStatus(List<Book> currentBooks, BookStatus status) {
        return currentBooks.stream().filter(book -> book.getStatus() == status).count();
    }

    private void updateRecentBooks(List<Book> currentBooks) {
        recentTable.getItems().setAll(currentBooks.reversed().stream().limit(5).toList());
        if (currentBooks.isEmpty()) {
            recentTable.setPlaceholder(new Label("Your collection is empty. Add a book to see it here."));
        }
    }

    private void setCompactRows(boolean compact) {
        for (TableView<Book> table : List.of(managementTable, recentTable)) {
            table.getStyleClass().remove("compact");
            if (compact) table.getStyleClass().add("compact");
        }
    }
}
