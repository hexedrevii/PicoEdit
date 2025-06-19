package xyz.itseve.picoedit.controllers;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import xyz.itseve.picoedit.utils.LuaHighlighter;
import xyz.itseve.picoedit.models.TabData;
import xyz.itseve.picoedit.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class EditorController implements Initializable {
    private File openedDir;

    private boolean allowHighlight = true;
    @FXML private void toggleHighlight() {
        allowHighlight = !allowHighlight;

        Tab sel = tabbedView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        CodeArea editor = (CodeArea)sel.getContent();

        if (allowHighlight) {
            Boolean isLuaFile = (Boolean) editor.getUserData();
            if (isLuaFile == null || !isLuaFile) return;

            StyleSpans<Collection<String>> spans = LuaHighlighter.computeHighlighting(editor.getText());
            editor.setStyleSpans(0, spans);

            return;
        }

        editor.setStyleSpans(0, new StyleSpansBuilder<Collection<String>>()
            .add(Collections.singleton("default"), editor.getText().length())
            .create()
        );
    }

    @FXML private TreeView<File> folderView;
    @FXML private TabPane tabbedView;

    private Stage mainStage = null;
    public void setMainStage(Stage stage) {
        mainStage = stage;

        // Set closure properties
        mainStage.setOnCloseRequest((event) -> {
            noticeUnsaved();
        });

        // Set focused properties
        mainStage.focusedProperty().addListener((obs, lost, found) -> {
            // Focus was regained.
            if (found) {
                // Close tabs if the file is gone.
                Utilities.removeTabsIfNotExistsWithEvent(tabbedView);

                // Recreate the tree view
                if (openedDir != null) {
                    // Kill
                    folderView.setRoot(null);

                    TreeItem<File> root = new TreeItem<>(openedDir);
                    root.setExpanded(true);

                    folderView.setRoot(root);

                    // Create the file view.
                    Utilities.createChildren(root, ignorePatterns);

                    // Show all directories first
                    root.getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
                }

                // Check if file was modified while we were out.
                for (Tab tab : tabbedView.getTabs()) {
                    TabData data = (TabData)tab.getUserData();
                    if (data.lastModified != data.getAssociated().lastModified()) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setHeaderText(null);
                        alert.setTitle("File modified");
                        alert.setContentText("The file " + data.getAssociated().getName() + " was modified. What would you like to do?");

                        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        ButtonType delete = new ButtonType("Discard your changes", ButtonBar.ButtonData.FINISH);
                        ButtonType apply = new ButtonType("Overwrite with your changes.", ButtonBar.ButtonData.APPLY);
                        alert.getButtonTypes().setAll(delete, cancel, apply);

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent()) {
                            ButtonType btn = result.get();
                            CodeArea editor = (CodeArea)tab.getContent();

                            if (btn.getButtonData() == ButtonBar.ButtonData.FINISH) {
                                try {
                                    editor.replaceText(Files.readString(Path.of(data.getAssociated().getAbsolutePath())));
                                    mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ")");

                                } catch (IOException e) {
                                    Alert error = new Alert(Alert.AlertType.ERROR);
                                    error.setTitle("Unable to read file");
                                    error.setContentText("The given file was unable to be read: " + e.getMessage());
                                    error.setHeaderText(null);

                                    error.showAndWait();
                                }
                            } else if (btn.getButtonData() == ButtonBar.ButtonData.APPLY) {
                                try {
                                    Files.writeString(data.getAssociated().toPath(), editor.getText());
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
                        }

                        data.lastModified = data.getAssociated().lastModified();
                    }
                }
            }
        });
    }

    // Hardcoded ignore patterns...
    private List<String> ignorePatterns = new ArrayList<String>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ignorePatterns = List.of(".git");

        SplitPane.setResizableWithParent(folderView, false);

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

                // Context menu.
                if (item.isDirectory()) {
                    MenuItem nfile = new MenuItem("Add file");
                    nfile.setOnAction(a -> {
                        TextInputDialog nf = new TextInputDialog();
                        nf.setTitle("Create new file");
                        nf.setHeaderText(null);

                        nf.showAndWait().ifPresent(name -> {
                            File toCreate = new File(item, name);
                            try {
                                if (toCreate.createNewFile()) {
                                    getTreeItem().getChildren().add(new TreeItem<>(toCreate));
                                    getTreeItem().setExpanded(true);

                                    getTreeItem().getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
                                } else {
                                    Utilities.showBasicError("Could not create file", "File already exists or could not be created.");
                                }
                            } catch (IOException e) {
                                Utilities.showBasicError("Could not create file", e.getMessage());
                            }
                        });
                    });

                    MenuItem ndir = new MenuItem("Add directory");
                    ndir.setOnAction(a -> {
                        TextInputDialog nf = new TextInputDialog();
                        nf.setTitle("Create new directory");
                        nf.setHeaderText(null);

                        nf.showAndWait().ifPresent(name -> {
                            File toCreate = new File(item, name);
                            if (toCreate.mkdir()) {
                                getTreeItem().getChildren().add(new TreeItem<>(toCreate));
                                getTreeItem().setExpanded(true);

                                getTreeItem().getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
                            } else {
                                Utilities.showBasicError("Could not create file", "File already exists or could not be created.");
                            }
                        });
                    });

                    MenuItem rn = new MenuItem("Rename directory");
                    rn.setOnAction(a -> {
                        TextInputDialog nf = new TextInputDialog();
                        nf.setTitle("Rename directory");
                        nf.setHeaderText(null);

                        nf.showAndWait().ifPresent(name -> {
                            File toRename = new File(item.getParent(), name);
                            if (item.renameTo(toRename)) {
                                // Set the new item
                                getTreeItem().setValue(toRename);

                                // Update child paths
                                Utilities.updateChildrenRecursive(getTreeItem(), item.toPath(), toRename.toPath());

                                Utilities.removeTabsIfNotExistsWithEvent(tabbedView);
                            } else {
                                Utilities.showBasicError("Directory could not be renamed.", "Directory already exists or another error occurred.");
                            }
                        });
                    });

                    MenuItem del = new MenuItem("Delete directory");
                    del.setOnAction(a -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setHeaderText(null);
                        alert.setTitle("Are you sure?");
                        alert.setContentText("This action is permanent and cannot be reversed. Delete anyway?");

                        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.APPLY);
                        alert.getButtonTypes().setAll(delete, cancel);

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.APPLY) {
                            try {
                                FileUtils.deleteDirectory(item);

                                // Erase the item
                                TreeItem<File> self = getTreeItem();
                                TreeItem<File> parent = self.getParent();

                                if (parent != null) {
                                    parent.getChildren().remove(self);
                                    parent.getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
                                } else {
                                    folderView.setRoot(null);
                                }

                                Utilities.removeTabsIfNotExistsWithEvent(tabbedView);
                            } catch (IOException e) {
                                Utilities.showBasicError("Could not delete directory", "The directory could not be deleted " + e.getMessage());
                            }
                        }
                    });

                    setContextMenu(new ContextMenu(nfile, ndir, rn, del));
                } else if (item.isFile()) {
                    MenuItem rn = new MenuItem("Rename file");
                    rn.setOnAction(a -> {
                        TextInputDialog nf = new TextInputDialog();
                        nf.setTitle("Rename directory");
                        nf.setHeaderText(null);

                        nf.showAndWait().ifPresent(name -> {
                            File toRename = new File(item.getParent(), name);
                            if (item.renameTo(toRename)) {
                                // Set the new item
                                getTreeItem().setValue(toRename);

                                Utilities.removeTabsIfNotExistsWithEvent(tabbedView);
                            } else {
                                Utilities.showBasicError("File could not be renamed.", "File already exists or another error occurred.");
                            }
                        });
                    });

                    MenuItem del = new MenuItem("Delete file");
                    del.setOnAction(a -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setHeaderText(null);
                        alert.setTitle("Are you sure?");
                        alert.setContentText("This action is permanent and cannot be reversed. Delete anyway?");

                        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.APPLY);
                        alert.getButtonTypes().setAll(delete, cancel);

                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.APPLY) {
                            try {
                                Files.delete(item.toPath());

                                // Erase the item
                                TreeItem<File> self = getTreeItem();
                                TreeItem<File> parent = self.getParent();

                                if (parent != null) {
                                    parent.getChildren().remove(self);
                                } else {
                                    folderView.setRoot(null);
                                }

                                Utilities.removeTabsIfNotExistsWithEvent(tabbedView);
                            } catch (IOException e) {
                                Utilities.showBasicError("Could not delete file.", "File could not be deleted: " + e.getMessage());
                            }
                        };
                    });

                    setContextMenu(new ContextMenu(rn, del));
                }
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

            CodeArea editor = new CodeArea();
            editor.setUserData(file.getName().endsWith(".lua") || file.getName().endsWith(".p8"));
            editor.setStyle(
                "-fx-font-family: 'PICO-8_REVERSE'; -fx-font-size: 18px; -fx-background-color: -fx-pico1; -fx-margin: 0;"
            );

            editor.textProperty().addListener((obs, oldText, newText) -> {
                if (!allowHighlight) {
                    Platform.runLater(() -> {
                        editor.setStyleSpans(0, StyleSpans.singleton(Collections.singleton("default"), newText.length()));
                    });
                    return;
                }

                Boolean isLuaFile = (Boolean) editor.getUserData();
                if (isLuaFile == null || !isLuaFile) {
                    Platform.runLater(() -> {
                        editor.setStyleSpans(0, StyleSpans.singleton(Collections.singleton("default"), newText.length()));
                    });
                    return;
                }

                Platform.runLater(() -> {
                    editor.setStyleSpans(0, LuaHighlighter.computeHighlighting(newText));
                });
            });

            TabData data = (TabData)tab.getUserData();
            data.lastModified = file.lastModified();

            try {
                editor.replaceText(Files.readString(Path.of(selected.getValue().getAbsolutePath())));
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

    private void noticeUnsaved() {
        for (Tab tab : tabbedView.getTabs()) {
            TabData data = (TabData)tab.getUserData();
            if (!data.modified) continue;

            // Unsaved file
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText(null);

            alert.setTitle("Unsaved changes");
            alert.setContentText("The file " + data.getAssociated().getName() + " has unsaved changes.");

            ButtonType discard = new ButtonType("Discard changes");
            ButtonType apply = new ButtonType("Apply changes", ButtonBar.ButtonData.APPLY);
            alert.getButtonTypes().setAll(discard, apply);

            Optional<ButtonType> opt = alert.showAndWait();
            if (opt.isEmpty()) continue;

            ButtonType btn = opt.get();
            CodeArea editor = (CodeArea)tab.getContent();
            if (btn.getButtonData() == ButtonBar.ButtonData.APPLY) {
                try {
                    Files.writeString(data.getAssociated().toPath(), editor.getText());
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
        }
    }

    public void handleExitRequest() {
        noticeUnsaved();

        mainStage.close();
    }

    public void handleSave() {
        Tab selected = tabbedView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CodeArea content = (CodeArea)selected.getContent();
        TabData data = (TabData)selected.getUserData();

        try {
            Files.writeString(data.getAssociated().toPath(), content.getText());
            data.modified = false;
            data.lastModified = data.getAssociated().lastModified();

            mainStage.setTitle("PicoEditor (" + data.getAssociated().getName() + ")");
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Unable to write file");
            error.setContentText("The program could not write to the specified file " + e.getMessage());
            error.setHeaderText(null);

            error.showAndWait();
        }
    }

    @FXML
    private void openFolder() {
        if (mainStage == null) throw new RuntimeException("Main parent is empty.");

        DirectoryChooser dir = new DirectoryChooser();
        dir.setTitle("Choose folder.");

        // Set the starting directory to the respective lexaloffle carts directory.
        // ONLY if it exists.
        File cartsDir = null;

        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) cartsDir = new File(userHome, "AppData/Roaming/pico-8/carts");
        else if (os.contains("mac")) cartsDir = new File(userHome, "Library/Application Support/pico-8/carts");
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) cartsDir = new File(userHome, ".lexaloffle/pico-8/carts");

        if (cartsDir != null && cartsDir.exists() && cartsDir.isDirectory())
            dir.setInitialDirectory(cartsDir);

        File file = dir.showDialog(mainStage);
        if (file == null) return;
        if (!file.isDirectory()) return;

        openedDir = file;

        // Set root
        TreeItem<File> root = new TreeItem<>(file);
        root.setExpanded(true);

        folderView.setRoot(root);

        // Create the file view.
        Utilities.createChildren(root, ignorePatterns);

        // Show all directories first
        root.getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
    }
}