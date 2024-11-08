package com.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MusicPlayer extends Application {
    private MediaPlayer mediaPlayer;
    private ObservableList<Track> playlist;
    private int currentTrackIndex;
    private boolean isPlaying;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private Label currentTrackLabel;
    private Button playButton;
    private Slider progressSlider;
    private Slider volumeSlider;
    private TableView<Track> playlistTable;
    private boolean seeking;
    private boolean shuffle;
    private boolean repeat;
    private Image playImage;
    private Image pauseImage;
    private Image nextImage;
    private Image previousImage;
    private Image stopImage;

    private static class Track {
        private final SimpleStringProperty name;
        private final SimpleStringProperty duration;
        private final File file;

        public Track(File file, String duration) {
            this.file = file;
            this.name = new SimpleStringProperty(file.getName());
            this.duration = new SimpleStringProperty(duration);
        }

        public String getName() { return name.get(); }
        public String getDuration() { return duration.get(); }
        public File getFile() { return file; }
    }

    @Override
    public void start(Stage primaryStage) {
        loadImages();
        playlist = FXCollections.observableArrayList();
        currentTrackIndex = 0;
        isPlaying = false;
        seeking = false;
        shuffle = false;
        repeat = false;

        // Create main layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Create UI components
        VBox mainContent = createMainContent();
        root.setCenter(mainContent);

        // Create scene with dark theme
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(MusicPlayer.class.getResource("resources/styles.css")).toExternalForm());

        primaryStage.setTitle("Modern Music Player");
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
    private void loadImages() {
        playImage = new Image(getClass().getResourceAsStream("resources/play.png"));
        pauseImage = new Image(getClass().getResourceAsStream("resources/pause.png"));
        nextImage = new Image(getClass().getResourceAsStream("resources/next.png"));
        previousImage = new Image(getClass().getResourceAsStream("resources/previous.png"));
        stopImage = new Image(getClass().getResourceAsStream("resources/stop.png"));
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Current track info
        VBox trackInfo = createTrackInfoSection();

        // Playlist
        VBox playlistSection = createPlaylistSection();

        // Controls
        VBox controlsSection = createControlsSection();

        mainContent.getChildren().addAll(trackInfo, controlsSection, playlistSection);
        return mainContent;
    }

    private VBox createTrackInfoSection() {
        VBox trackInfo = new VBox(10);
        trackInfo.setAlignment(Pos.CENTER);

        currentTrackLabel = new Label("No track selected");
        currentTrackLabel.getStyleClass().add("track-title");

        HBox timeInfo = new HBox(10);
        timeInfo.setAlignment(Pos.CENTER);
        currentTimeLabel = new Label("00:00");
        totalTimeLabel = new Label("00:00");
        timeInfo.getChildren().addAll(currentTimeLabel, new Label("/"), totalTimeLabel);

        trackInfo.getChildren().addAll(currentTrackLabel, timeInfo);
        return trackInfo;
    }

    private VBox createPlaylistSection() {
        VBox playlistSection = new VBox(10);
        playlistSection.setAlignment(Pos.CENTER);

        // Create playlist table
        playlistTable = new TableView<>();
        playlistTable.setStyle("-fx-background-color: #2a2a2a;");

        TableColumn<Track, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Track, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDuration()));

        playlistTable.getColumns().addAll(nameColumn, durationColumn);
        playlistTable.setItems(playlist);
        playlistTable.setPrefHeight(200);

        // Double click to play
        playlistTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Track selectedTrack = playlistTable.getSelectionModel().getSelectedItem();
                if (selectedTrack != null) {
                    currentTrackIndex = playlist.indexOf(selectedTrack);
                    loadAndPlayTrack(selectedTrack.getFile());
                }
            }
        });

        // Add song button
        Button addButton = new Button("Add Songs");
        addButton.getStyleClass().add("modern-button");
        addButton.setOnAction(e -> addSongs());

        playlistSection.getChildren().addAll(new Label("Playlist"), playlistTable, addButton);
        return playlistSection;
    }

    private VBox createControlsSection() {
        VBox controlsSection = new VBox(10);
        controlsSection.setAlignment(Pos.CENTER);

        // Progress slider
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(600);
        progressSlider.setDisable(true);
        setupProgressSlider();

        // Control buttons
        HBox controlButtons = createControlButtons();

        // Volume control
        HBox volumeControl = createVolumeControl();

        controlsSection.getChildren().addAll(progressSlider, controlButtons, volumeControl);
        return controlsSection;
    }

    private void setupProgressSlider() {
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (seeking && mediaPlayer != null) {
                double duration = mediaPlayer.getTotalDuration().toSeconds();
                double seekTime = duration * (newVal.doubleValue() / 100.0);
                mediaPlayer.seek(Duration.seconds(seekTime));
            }
        });
        progressSlider.setOnMousePressed(e -> seeking = true);
        progressSlider.setOnMouseReleased(e -> seeking = false);
    }


    private HBox createControlButtons() {
        HBox controlBox = new HBox(20);
        controlBox.setAlignment(Pos.CENTER);

        // Create ImageViews
        ImageView previousImageView = new ImageView(previousImage);
        ImageView playImageView = new ImageView(playImage);
        ImageView stopImageView = new ImageView(stopImage);
        ImageView nextImageView = new ImageView(nextImage);

        // Set size for all images
        previousImageView.setFitWidth(24);
        previousImageView.setFitHeight(24);
        playImageView.setFitWidth(32);
        playImageView.setFitHeight(32);
        stopImageView.setFitWidth(24);
        stopImageView.setFitHeight(24);
        nextImageView.setFitWidth(24);
        nextImageView.setFitHeight(24);

        // Preserve ratio for all images
        previousImageView.setPreserveRatio(true);
        playImageView.setPreserveRatio(true);
        stopImageView.setPreserveRatio(true);
        nextImageView.setPreserveRatio(true);

        // Create buttons with images
        Button shuffleButton = new Button("Shuffle");
        Button previousButton = new Button();
        playButton = new Button();
        Button stopButton = new Button();
        Button nextButton = new Button();
        Button repeatButton = new Button("Repeat");

        // Set images to buttons
        previousButton.setGraphic(previousImageView);
        playButton.setGraphic(playImageView);
        stopButton.setGraphic(stopImageView);
        nextButton.setGraphic(nextImageView);

        // Add style classes
        shuffleButton.getStyleClass().add("control-button");
        previousButton.getStyleClass().add("image-button");
        playButton.getStyleClass().add("play-button");
        stopButton.getStyleClass().add("image-button");
        nextButton.getStyleClass().add("image-button");
        repeatButton.getStyleClass().add("control-button");

        // Setup button actions
        shuffleButton.setOnAction(e -> toggleShuffle());
        previousButton.setOnAction(e -> playPrevious());
        playButton.setOnAction(e -> togglePlay());
        stopButton.setOnAction(e -> stopMusic());
        nextButton.setOnAction(e -> playNext());
        repeatButton.setOnAction(e -> toggleRepeat());

        controlBox.getChildren().addAll(
                shuffleButton,
                previousButton,
                playButton,
                stopButton,
                nextButton,
                repeatButton
        );
        return controlBox;
    }


    private HBox createVolumeControl() {
        HBox volumeBox = new HBox(10);
        volumeBox.setAlignment(Pos.CENTER);

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        Label volumeIcon = new Label("🔊");
        volumeBox.getChildren().addAll(volumeIcon, volumeSlider);
        return volumeBox;
    }

    private void addSongs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                Media media = new Media(file.toURI().toString());
                MediaPlayer tempPlayer = new MediaPlayer(media);

                tempPlayer.setOnReady(() -> {
                    Duration duration = tempPlayer.getMedia().getDuration();
                    String formattedDuration = formatDuration(duration);
                    playlist.add(new Track(file, formattedDuration));
                    tempPlayer.dispose();
                });
            }

            if (mediaPlayer == null && !playlist.isEmpty()) {
                loadAndPlayTrack(playlist.get(0).getFile());
            }
        }
    }

    private void loadAndPlayTrack(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            setupMediaPlayer();

            currentTrackLabel.setText(file.getName());
            progressSlider.setDisable(false);
            mediaPlayer.setVolume(volumeSlider.getValue());

            if (isPlaying) {
                mediaPlayer.play();
            }
        } catch (Exception e) {
            showError("Error loading media file: " + e.getMessage());
        }
    }

    private void setupMediaPlayer() {
        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!seeking) {
                double progress = (newVal.toSeconds() / mediaPlayer.getTotalDuration().toSeconds()) * 100;
                progressSlider.setValue(progress);
                currentTimeLabel.setText(formatDuration(newVal));
                totalTimeLabel.setText(formatDuration(mediaPlayer.getTotalDuration()));
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (repeat) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else {
                playNext();
            }
        });
    }

    private void togglePlay() {
        if (playlist.isEmpty()) {
            showError("Please add songs to the playlist first!");
            return;
        }

        if (mediaPlayer == null) {
            loadAndPlayTrack(playlist.get(currentTrackIndex).getFile());
        }

        if (isPlaying) {
            mediaPlayer.pause();
            ((ImageView) playButton.getGraphic()).setImage(playImage);
        } else {
            mediaPlayer.play();
            ((ImageView) playButton.getGraphic()).setImage(pauseImage);
        }
        isPlaying = !isPlaying;
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            ((ImageView) playButton.getGraphic()).setImage(playImage);
            progressSlider.setValue(0);
        }
    }

    private void playNext() {
        if (!playlist.isEmpty()) {
            if (shuffle) {
                currentTrackIndex = (int) (Math.random() * playlist.size());
            } else {
                currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
            }
            loadAndPlayTrack(playlist.get(currentTrackIndex).getFile());
        }
    }

    private void playPrevious() {
        if (!playlist.isEmpty()) {
            if (shuffle) {
                currentTrackIndex = (int) (Math.random() * playlist.size());
            } else {
                currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
            }
            loadAndPlayTrack(playlist.get(currentTrackIndex).getFile());
        }
    }

    private void toggleShuffle() {
        shuffle = !shuffle;
        // Update UI to show shuffle state
    }

    private void toggleRepeat() {
        repeat = !repeat;
        // Update UI to show repeat state
    }

    private String formatDuration(Duration duration) {
        long seconds = (long) duration.toSeconds();
        return String.format("%02d:%02d",
                TimeUnit.SECONDS.toMinutes(seconds),
                seconds % TimeUnit.MINUTES.toSeconds(1)
        );
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