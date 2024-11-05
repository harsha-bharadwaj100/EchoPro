module EchoPro {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.graphics;
    opens com.musicplayer to javafx.graphics;
    exports com.musicplayer;
}