package xyz.itseve.picoedit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xyz.itseve.picoedit.controllers.EditorController;

import java.io.IOException;
import java.net.URL;

public class Entry extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Entry.class.getResource("editor.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        EditorController controller = loader.getController();
        controller.setMainStage(stage);

        stage.setTitle("PicoEdit");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}