package xyz.itseve.picoedit.models;

import java.io.File;

public class TabData {
    public boolean firstEdited = false;
    public boolean modified = false;

    private final File associated;
    public File getAssociated() {
        return associated;
    }

    public long lastModified;

    public TabData(File file) {
        associated = file;
    }
}
