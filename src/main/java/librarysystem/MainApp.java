package librarysystem;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import librarysystem.controller.LibraryController;
import librarysystem.service.LibraryService;
import librarysystem.view.MainView;

/** JavaFX entry point for the Library Book Management System. */
public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        LibraryService service = new LibraryService();
        MainView view = new MainView();
        new LibraryController(service, view);

        Scene scene = new Scene(view.getRoot(), 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles/main.css").toExternalForm());
        primaryStage.setTitle("Library Book Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
