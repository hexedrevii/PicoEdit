package xyz.itseve.picoedit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Entry extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = loadScene(
            Entry.class.getResource("editor.fxml"),
            800, 600
        );

        stage.setTitle("PicoEdit");
        stage.setScene(scene);
        stage.show();
    }

    private Scene loadScene(URL path, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(path);
        return new Scene(loader.load(), width, height);
    }

    public static void main(String[] args) {
        launch();
    }
}