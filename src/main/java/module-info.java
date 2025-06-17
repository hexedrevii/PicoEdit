module xyz.itseve.picoedit {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.commons.io;
    requires org.fxmisc.richtext;

    opens xyz.itseve.picoedit to javafx.fxml;

    exports xyz.itseve.picoedit;
    exports xyz.itseve.picoedit.controllers;

    opens xyz.itseve.picoedit.controllers to javafx.fxml;
}