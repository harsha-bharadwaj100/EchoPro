package com.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayer extends Application {
    private MediaPlayer mediaPlayer;
    private List<File> playlist;
    private int currentTrackIndex;
    private boolean isPlaying;
    private Label currentTrackLabel;
    private Button playButton;
    private Slider progressSlider;
    private Slider volumeSlider;
    private boolean seeking;

    @Override
    public void start(Stage primaryStage) {
        playlist = new ArrayList<>();
        currentTrackIndex = 0;
        isPlaying = false;
        seeking = false;

        // Create UI components
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Menu
        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        // Current track label
        currentTrackLabel = new Label("No track selected");
        currentTrackLabel.setAlignment(Pos.CENTER);
        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().add(currentTrackLabel);

        // Progress slider
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(300);
        progressSlider.setDisable(true);
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (seeking && mediaPlayer != null) {
                double duration = mediaPlayer.getTotalDuration().toSeconds();
                double seekTime = duration * (newVal.doubleValue() / 100.0);
                mediaPlayer.seek(Duration.seconds(seekTime));
            }
        });
        progressSlider.setOnMousePressed(e -> seeking = true);
        progressSlider.setOnMouseReleased(e -> seeking = false);
        centerBox.getChildren().add(progressSlider);

        // Control buttons
        HBox controlBox = createControlButtons();
        centerBox.getChildren().add(controlBox);

        // Volume control
        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        volumeBox.getChildren().addAll(new Label("Volume:"), volumeSlider);
        centerBox.getChildren().add(volumeBox);

        root.setCenter(centerBox);

        // Create scene
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("MP3 Music Player");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
            }
            Platform.exit();
        });
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem addItem = new MenuItem("Add Song");
        addItem.setOnAction(e -> addSong(stage));
        fileMenu.getItems().add(addItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private HBox createControlButtons() {
        HBox controlBox = new HBox(10);
        controlBox.setAlignment(Pos.CENTER);

        Button previousButton = new Button("Previous");
        playButton = new Button("Play");
        Button stopButton = new Button("Stop");
        Button nextButton = new Button("Next");

        previousButton.setOnAction(e -> playPrevious());
        playButton.setOnAction(e -> togglePlay());
        stopButton.setOnAction(e -> stopMusic());
        nextButton.setOnAction(e -> playNext());

        controlBox.getChildren().addAll(previousButton, playButton, stopButton, nextButton);
        return controlBox;
    }

    private void addSong(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
        );
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            playlist.add(selectedFile);
            if (playlist.size() == 1) {
                currentTrackLabel.setText(selectedFile.getName());
                loadMedia(selectedFile);
            }
        }
    }

    private void loadMedia(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                if (!seeking) {
                    double progress = (newVal.toSeconds() / mediaPlayer.getTotalDuration().toSeconds()) * 100;
                    progressSlider.setValue(progress);
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                playNext();
            });

            progressSlider.setDisable(false);
            mediaPlayer.setVolume(volumeSlider.getValue());

        } catch (Exception e) {
            showError("Error loading media file: " + e.getMessage());
        }
    }

    private void togglePlay() {
        if (playlist.isEmpty()) {
            showError("Please add songs to the playlist first!");
            return;
        }

        if (mediaPlayer == null) {
            loadMedia(playlist.get(currentTrackIndex));
        }

        if (isPlaying) {
            mediaPlayer.pause();
            playButton.setText("Play");
        } else {
            mediaPlayer.play();
            playButton.setText("Pause");
        }
        isPlaying = !isPlaying;
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            playButton.setText("Play");
            progressSlider.setValue(0);
        }
    }

    private void playNext() {
        if (!playlist.isEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
            File nextFile = playlist.get(currentTrackIndex);
            currentTrackLabel.setText(nextFile.getName());
            loadMedia(nextFile);
            if (isPlaying) {
                mediaPlayer.play();
            }
        }
    }

    private void playPrevious() {
        if (!playlist.isEmpty()) {
            currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
            File prevFile = playlist.get(currentTrackIndex);
            currentTrackLabel.setText(prevFile.getName());
            loadMedia(prevFile);
            if (isPlaying) {
                mediaPlayer.play();
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}