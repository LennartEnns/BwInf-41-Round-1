package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Main-Klasse der JavaFX-Anwendung */
public class JavaFXMain extends Application{

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            Platform.setImplicitExit(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
            Parent root = loader.load();
            root.setOnMousePressed(e -> {
                root.requestFocus();
            });

            Controller controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root);
            stage.setTitle("Gap Text Solver");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                if (controller.checkGtSave() && controller.checkBtSave()) {
                    Platform.exit();
                } else {
                    event.consume();
                }
            });

            stage.show();
        } catch (Exception ignored) {}
    }
}