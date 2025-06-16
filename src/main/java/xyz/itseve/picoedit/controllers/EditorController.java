package xyz.itseve.picoedit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EditorController implements Initializable {
    private File openedDir;

    @FXML private TreeView<File> folderView;
    @FXML private TabPane tabbedView;

    private Stage mainStage = null;
    public void setMainStage(Stage stage) {
        mainStage = stage;
    }

    // Hardcoded ignore patterns...
    private List<String> ignorePatterns = new ArrayList<String>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ignorePatterns = List.of(".git");

        folderView.setShowRoot(true);

        // Prioritise displaying by filename.
        folderView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(
                    item.getName().isEmpty() ?
                        item.getAbsolutePath() :
                        item.getName()
                );
            }
        });

        // double click to open tab
        folderView.setOnMouseClicked(event -> {
            if (event.getClickCount() < 2) return;

            TreeItem<File> selected = folderView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            File file = selected.getValue();
            if (!file.isFile()) return;

            String displayName = file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();

            for (Tab tab : tabbedView.getTabs()) {
                if (displayName.equals(tab.getText())) {
                    tabbedView.getSelectionModel().select(tab);
                    return;
                }
            }

            // Create new tab
            Tab tab = new Tab();
            tab.setText(displayName);

            TextArea editor = new TextArea();
            SplitPane.setResizableWithParent(editor, true);

            try {
                editor.setText(Files.readString(Path.of(selected.getValue().getAbsolutePath())));
            } catch (IOException e) {
                throw new RuntimeException("Could not open file: " + e.getMessage());
            }

            tab.setContent(editor);
            tabbedView.getTabs().add(tab);

            // Focus new tab
            tabbedView.getSelectionModel().select(tab);
        });
    }

    private void createChildren(TreeItem<File> root) {
        File[] rootFiles = root.getValue().listFiles();
        if (rootFiles == null) return;

        for (File child : rootFiles) {
            // Skip child if it is in the ignore patterns.
            if (!child.getName().isEmpty()) {
                if (ignorePatterns.contains(child.getName())) {
                    continue;
                }
            }

            if (child.isFile()) {
                root.getChildren().add(
                        new TreeItem<File>(child)
                );

                continue;
            }

            TreeItem<File> newRoot = new TreeItem<File>(child);
            root.getChildren().add(newRoot);

            // New directory.
            createChildren(newRoot);
        }
    }

    @FXML
    private void openFolder() {
        if (mainStage == null) {
            throw new RuntimeException("Main parent is empty.");
        }

        DirectoryChooser dir = new DirectoryChooser();
        dir.setTitle("Choose folder.");

        File file = dir.showDialog(mainStage);
        if (file == null) return;
        if (!file.isDirectory()) return;

        openedDir = file;

        // Set root
        TreeItem<File> root = new TreeItem<>(file);
        root.setExpanded(true);

        folderView.setRoot(root);

        // Create the file view.
        createChildren(root);

        // Show all directories first
        root.getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
    }
}