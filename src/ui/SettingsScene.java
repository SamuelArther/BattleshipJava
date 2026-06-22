package ui;

import audio.AudioManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import settings.Settings;

/**
 * Video and audio settings.
 *
 * Changes take effect as they are made rather than on an Apply button: the volume sliders
 * retune whatever is playing, and the display controls resize or restyle the window straight
 * away, so the player can hear and see what a setting does before deciding to keep it.
 */
public class SettingsScene {

    /** Called when a display setting changes and the window has to be rebuilt or resized. */
    @FunctionalInterface
    public interface DisplayChangeHandler {
        void displayChanged();
    }

    private final AudioManager audioManager;
    private final Runnable backAction;
    private final DisplayChangeHandler displayChangeHandler;
    private final Settings settings = Settings.get();
    private Scene scene;

    public SettingsScene(AudioManager audioManager, Runnable backAction, DisplayChangeHandler displayChangeHandler) {
        this.audioManager = audioManager;
        this.backAction = backAction;
        this.displayChangeHandler = displayChangeHandler;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        Label title = UiFactory.createScreenTitle("Settings");

        VBox content = new VBox(20, title, buildVideoPanel(), buildAudioPanel(), buildBackButton());
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(760);
        content.setPadding(new Insets(4, 18, 18, 4));

        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("page-scroll");

        root.getChildren().add(scroller);
        scene = new Scene(root, settings.getWindowWidth(), settings.getWindowHeight());
        return scene;
    }

    private VBox buildBackButton() {
        VBox box = new VBox(UiFactory.createMenuButton("Back", audioManager, () -> {
            settings.save();
            backAction.run();
        }));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox buildThemeRow() {
        ComboBox<Settings.Theme> themeBox = new ComboBox<>();
        themeBox.getItems().addAll(Settings.Theme.values());
        themeBox.getSelectionModel().select(settings.getTheme());
        themeBox.setPrefWidth(280);

        Label description = new Label(settings.getTheme().getDescription());
        description.getStyleClass().add("muted-text");
        description.setWrapText(true);

        themeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            settings.setTheme(newValue);
            settings.save();
            description.setText(newValue.getDescription());
            // A palette only redefines colour tokens, so reloading the stylesheets on the
            // live scene recolours everything in place without rebuilding the screen.
            if (scene != null) {
                UiFactory.applyTheme(scene);
            }
        });

        VBox rows = new VBox(12, labelled("Theme", themeBox), description);
        return rows;
    }

    // ── Video ───────────────────────────────────────────────────────────────

    private VBox buildVideoPanel() {
        Label heading = UiFactory.createSectionTitle("Video");

        ComboBox<Settings.DisplayMode> modeBox = new ComboBox<>();
        modeBox.getItems().addAll(Settings.DisplayMode.values());
        modeBox.getSelectionModel().select(settings.getDisplayMode());
        modeBox.setPrefWidth(280);

        Label modeDescription = new Label(settings.getDisplayMode().getDescription());
        modeDescription.getStyleClass().add("muted-text");
        modeDescription.setWrapText(true);

        ComboBox<Settings.WindowSize> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(Settings.WINDOW_SIZES);
        sizeBox.getSelectionModel().select(matchingSize());
        sizeBox.setPrefWidth(280);
        sizeBox.setDisable(settings.getDisplayMode() != Settings.DisplayMode.WINDOWED);

        Label sizeNote = new Label("Window size only applies in windowed mode. "
            + "The other two modes always fill the screen the game is on.");
        sizeNote.getStyleClass().add("muted-text");
        sizeNote.setWrapText(true);

        modeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            settings.setDisplayMode(newValue);
            modeDescription.setText(newValue.getDescription());
            sizeBox.setDisable(newValue != Settings.DisplayMode.WINDOWED);
            settings.save();
            displayChangeHandler.displayChanged();
        });

        sizeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            settings.setWindowSize(newValue);
            settings.save();
            displayChangeHandler.displayChanged();
        });

        VBox panel = new VBox(12,
            heading,
            buildThemeRow(),
            labelled("Display mode", modeBox),
            modeDescription,
            labelled("Window size", sizeBox),
            sizeNote);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        return panel;
    }

    private Settings.WindowSize matchingSize() {
        for (Settings.WindowSize size : Settings.WINDOW_SIZES) {
            if (size.width() == settings.getWindowWidth() && size.height() == settings.getWindowHeight()) {
                return size;
            }
        }
        return Settings.WINDOW_SIZES[1];
    }

    // ── Audio ───────────────────────────────────────────────────────────────

    private VBox buildAudioPanel() {
        Label heading = UiFactory.createSectionTitle("Audio");

        VBox panel = new VBox(12,
            heading,
            volumeRow("Master volume", settings.getMasterVolume(), value -> {
                settings.setMasterVolume(value);
                audioManager.refreshVolumes();
            }),
            volumeRow("Music", settings.getMusicVolume(), value -> {
                settings.setMusicVolume(value);
                audioManager.refreshVolumes();
            }),
            volumeRow("Sound effects", settings.getEffectsVolume(), value -> {
                settings.setEffectsVolume(value);
                audioManager.playSelect();
            }));
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        return panel;
    }

    private HBox volumeRow(String labelText, double initial, java.util.function.DoubleConsumer onChange) {
        Label label = new Label(labelText);
        label.getStyleClass().add("body-text");
        label.setMinWidth(150);

        Slider slider = new Slider(0, 1, initial);
        slider.setPrefWidth(320);
        HBox.setHgrow(slider, Priority.ALWAYS);

        Label readout = new Label(percent(initial));
        readout.getStyleClass().add("muted-text");
        readout.setMinWidth(52);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            readout.setText(percent(newValue.doubleValue()));
            onChange.accept(newValue.doubleValue());
        });
        // Save once the player lets go, rather than on every pixel of the drag.
        slider.valueChangingProperty().addListener((observable, wasChanging, changing) -> {
            if (!changing) {
                settings.save();
            }
        });
        slider.setOnMouseReleased(event -> settings.save());

        HBox row = new HBox(14, label, slider, readout);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    private HBox labelled(String labelText, Region control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("body-text");
        label.setMinWidth(150);
        HBox row = new HBox(14, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
