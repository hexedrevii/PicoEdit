package xyz.itseve.picoedit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import xyz.itseve.picoedit.controllers.EditorController;

import java.io.IOException;
import java.util.Objects;

public class Entry extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Entry.class.getResource("editor.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        // Main UI interface font
        Font.loadFont(
            Objects.requireNonNull(
                Entry.class.getResource("fonts/pico-8_unreversed.ttf")
            ).toExternalForm(), 20
        );

        // Code editor font
        Font.loadFont(
            Objects.requireNonNull(
                Entry.class.getResource("fonts/pico-8_reversed.ttf")
            ).toExternalForm(), 20
        );

        EditorController controller = loader.getController();
        controller.setMainStage(stage);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        Entry.class.getResource("styles/editor.css")
                ).toExternalForm()
        );

        scene.getStylesheets().add(
            Objects.requireNonNull(
                Entry.class.getResource("styles/basic-highlight.css")
            ).toExternalForm()
        );

        // Save on Ctrl + S
        KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        scene.getAccelerators().put(ctrlS, controller::handleSave);

        stage.setTitle("PicoEdit");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}