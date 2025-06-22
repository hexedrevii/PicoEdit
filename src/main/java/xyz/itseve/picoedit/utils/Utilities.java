package xyz.itseve.picoedit.utils;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import xyz.itseve.picoedit.models.TabData;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Utilities {
    public static void showBasicError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.setHeaderText(null);

        alert.showAndWait();
    }

    public static void removeTabsIfNotExistsWithEvent(TabPane parent) {
        List<Tab> toRemove = new ArrayList<Tab>();
        for (Tab tab : parent.getTabs()) {
            TabData td = (TabData)tab.getUserData();
            if (!td.getAssociated().exists()) {
                EventHandler<Event> handler = tab.getOnCloseRequest();
                if (handler != null) {
                    Event closeEvent = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);
                    handler.handle(closeEvent);
                    if (closeEvent.isConsumed()) {
                        continue;
                    }
                }
                toRemove.add(tab);
            }
        }

        parent.getTabs().removeAll(toRemove);
    }

    public static void updateChildrenRecursive(TreeItem<File> parent, Path oldRoot, Path newRoot) {
        for (TreeItem<File> child : parent.getChildren()) {
            File old = child.getValue();
            Path rel = oldRoot.relativize(old.toPath());
            File nef = newRoot.resolve(rel).toFile();

            child.setValue(nef);

            if (!child.getChildren().isEmpty()) {
                updateChildrenRecursive(child, oldRoot, newRoot);
            }
        }
    }

    public static void createChildren(
        TreeItem<File> root, List<String> ignorePatterns, List<File> expandPatterns
    ) {
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
            if (expandPatterns.contains(newRoot.getValue())) {
                newRoot.setExpanded(true);
            }

            root.getChildren().add(newRoot);

            // New directory.
            createChildren(newRoot, ignorePatterns, expandPatterns);
        }
    }

    public static List<File> findAllExpandsRecursive(TreeItem<File> root) {
        List<File> expand = new ArrayList<File>();
        if (root.isExpanded()) {
            expand.add(root.getValue());
        }

        for (TreeItem<File> child : root.getChildren()) {
            if (child.isExpanded()) {
                expand.addAll(findAllExpandsRecursive(child));
            }
        }

        return expand;
    }

    public static void createTree(
        TreeView<File> parent, File rootFile,
        List<String> ignorePatterns, List<File> expandPatterns
    ) {
        TreeItem<File> root = new TreeItem<>(rootFile);
        root.setExpanded(true);

        parent.setRoot(root);

        // Create the file view.
        createChildren(root, ignorePatterns, expandPatterns);

        // Show all directories first
        root.getChildren().sort(Comparator.comparing(t -> t.getValue().isFile()));
    }
}
