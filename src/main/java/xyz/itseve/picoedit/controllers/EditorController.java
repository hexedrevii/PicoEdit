package xyz.itseve.picoedit.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import xyz.itseve.picoedit.TabData;

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

        // Switch window title when switching tabs
        tabbedView.getSelectionModel().selectedItemProperty().addListener((obs, newTab, oldTab) -> {
            if (oldTab == null) return;

            TabData data = (TabData)oldTab.getUserData();

            if (!data.modified) {
                mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ")");
            } else {
                mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ") *");
            }
        });

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

        // Double click to open tab
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

                    TabData data = (TabData)tab.getUserData();
                    mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ")");

                    return;
                }
            }

            // Create new tab
            Tab tab = new Tab();
            tab.setText(displayName);
            tab.setUserData(new TabData(file));

            tab.setOnCloseRequest(e -> {
                TabData data = (TabData)tab.getUserData();

                if (data.modified) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setHeaderText(null);

                    alert.setTitle("Unsaved changes");
                    alert.setContentText("The file " + data.getAssociated().getName() + " has unsaved changes. Close anyway?");

                    ButtonType save = new ButtonType("Save and Discard", ButtonBar.ButtonData.APPLY);
                    ButtonType discard = new ButtonType("Discard");
                    ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(save, discard, cancel);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isEmpty() || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                        e.consume();
                        return;
                    } else if (result.get().getButtonData() == ButtonBar.ButtonData.APPLY) {
                        handleSave();
                    }

                    // Predict the next tab so we can change the name
                    SingleSelectionModel<Tab> selection = tabbedView.getSelectionModel();
                    ObservableList<Tab> tabs = tabbedView.getTabs();

                    int closingIndex = tabs.indexOf(tab);
                    Tab nextTab = null;

                    // ???
                    if (tabs.size() > 1) {
                        if (selection.getSelectedItem() == tab) {
                            if (closingIndex > 0) {
                                nextTab = tabs.get(closingIndex - 1);
                            } else if (closingIndex < tabs.size() - 1) {
                                nextTab = tabs.get(closingIndex + 1); 
                            }
                        } else {
                            nextTab = selection.getSelectedItem();
                        }

                        if (nextTab != null) {
                            TabData ntd = (TabData)nextTab.getUserData();
                            if (ntd.modified) {
                                mainStage.setTitle("PicoEditor (" + ntd.getAssociated().getName() + ") *");
                            } else {
                                mainStage.setTitle("PicoEditor (" + ntd.getAssociated().getName() + ")");
                            }
                        } else {
                            mainStage.setTitle("PicoEditor");
                        }
                    }
                }
            });

            mainStage.setTitle("PicoEditor (" + file.getName() + ")");

            TextArea editor = new TextArea();
            SplitPane.setResizableWithParent(editor, true);

            TabData data = (TabData)tab.getUserData();

            try {
                editor.setText(Files.readString(Path.of(selected.getValue().getAbsolutePath())));
            } catch (IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Unable to read file");
                error.setContentText("The given file was unable to be read: " + e.getMessage());
                error.setHeaderText(null);

                error.showAndWait();

                return;
            }

            editor.textProperty().addListener((obs, oldText, newText) -> {
                if (!data.firstEdited) return;
                if (data.modified) return;

                data.modified = true;
                mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ") *");
            });

            data.firstEdited = true;

            tab.setContent(editor);
            tabbedView.getTabs().add(tab);

            // Focus new tab
            tabbedView.getSelectionModel().select(tab);
        });
    }

    public void handleSave() {
        Tab selected = tabbedView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextArea content = (TextArea)selected.getContent();
        TabData data = (TabData)selected.getUserData();

        try {
            Files.writeString(data.getAssociated().toPath(), content.getText());
            data.modified = false;

            mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ")");
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Unable to write file");
            error.setContentText("The program could not write to the specified file " + e.getMessage());
            error.setHeaderText(null);

            error.showAndWait();
        }
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