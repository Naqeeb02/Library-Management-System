package librarysystem;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
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

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1240, screen.getWidth() - 160);
        double height = Math.min(800, screen.getHeight() - 72);
        Scene scene = new Scene(view.getRoot(), width, height);
        scene.getStylesheets().add(
                getClass().getResource("/styles/main.css").toExternalForm());
        primaryStage.setTitle(view.getApplicationName());
        primaryStage.getIcons().add(view.createWindowIcon());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(Math.min(900, width));
        primaryStage.setMinHeight(Math.min(580, height));
        primaryStage.show();
        primaryStage.setX(screen.getMinX() + Math.max(0, (screen.getWidth() - width) / 2));
        primaryStage.setY(screen.getMinY() + Math.max(0, (screen.getHeight() - height) / 2));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
