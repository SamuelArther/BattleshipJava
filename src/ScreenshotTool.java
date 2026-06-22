import ai.Difficulty;
import audio.AudioManager;
import game.Board;
import game.GameMode;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import ui.EndScene;
import ui.GameScene;
import ui.MainMenuScene;
import ui.SetupScene;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renders each screen and writes it to docs/screenshots, so the pictures in the README
 * are the real interface rather than something drawn by hand that drifts out of date.
 *
 * Run with: ./gradlew screenshots
 */
public class ScreenshotTool extends Application {

    private static final File OUTPUT = new File("docs/screenshots");

    private Stage stage;
    private AudioManager audioManager;

    @Override
    public void start(Stage primaryStage) {
        OUTPUT.mkdirs();
        stage = primaryStage;
        stage.setTitle("Battleship");
        audioManager = new AudioManager();
        audioManager.stopMenuMusic();

        show(new MainMenuScene(audioManager, noop(), noop(), noop(), noop(), noop(), noop(), noop()).createScene());
        after(1.2, () -> {
            capture("menu.png");
            captureSetup();
        });
    }

    private void captureSetup() {
        SetupScene setup = new SetupScene(audioManager, GameMode.SINGLEPLAYER, noop(), (b, d) -> { }, (b, s, t) -> { });
        Scene scene = setup.createScene();
        show(scene);
        // Lay a fleet down so the placement grid is not an empty board.
        after(0.6, () -> {
            placeFleetThroughTheUi(scene);
            after(0.8, () -> {
                capture("setup.png");
                captureBattle();
            });
        });
    }

    private void captureBattle() {
        Board playerBoard = new Board();
        playerBoard.randomize(new Random(7));
        GameScene game = GameScene.createSinglePlayer(
            audioManager, playerBoard, Difficulty.US_NAVY, (title, message) -> { }, noop());
        Scene scene = game.createScene();
        show(scene);

        // Trade a few shots so the grids show hits, misses and a live status line.
        List<Button> enemyTiles = enemyTiles(scene);
        int[] picks = {44, 45, 12, 13, 67, 88, 21};
        fireSequence(enemyTiles, picks, 0, () -> {
            capture("battle.png");
            captureSettings();
        });
    }

    private void fireSequence(List<Button> tiles, int[] picks, int index, Runnable done) {
        if (index >= picks.length) {
            after(1.0, done);
            return;
        }
        Button tile = tiles.get(picks[index]);
        if (!tile.isDisabled()) {
            tile.fire();
        }
        after(2.6, () -> fireSequence(tiles, picks, index + 1, done));
    }

    private void captureSettings() {
        show(new ui.SettingsScene(audioManager, noop(), () -> { }).createScene());
        after(1.0, () -> {
            capture("settings.png");
            captureVictory();
        });
    }

    private void captureVictory() {
        show(new EndScene(audioManager, "Victory",
            "Every enemy ship is on the bottom. The fleet is yours.", noop(), noop()).createScene());
        after(1.2, () -> {
            capture("victory.png");
            captureThemes();
        });
    }

    private void captureThemes() {
        // setTheme does not persist on its own, so this leaves the player's choice alone.
        settings.Settings prefs = settings.Settings.get();
        settings.Settings.Theme original = prefs.getTheme();
        java.util.List<settings.Settings.Theme> themes =
            java.util.List.of(settings.Settings.Theme.values());
        captureTheme(themes, 0, () -> {
            prefs.setTheme(original);
            captureParty();
        });
    }

    private void captureTheme(java.util.List<settings.Settings.Theme> themes, int index, Runnable done) {
        if (index >= themes.size()) {
            done.run();
            return;
        }
        settings.Settings.Theme theme = themes.get(index);
        settings.Settings.get().setTheme(theme);
        Scene menu = new MainMenuScene(audioManager, noop(), noop(), noop(), noop(), noop(), noop(), noop()).createScene();
        show(menu);
        after(0.9, () -> {
            capture("theme-" + theme.name().toLowerCase() + ".png");
            captureTheme(themes, index + 1, done);
        });
    }

    private void captureParty() {
        Scene menu = new MainMenuScene(audioManager, noop(), noop(), noop(), noop(), noop(), noop(), noop()).createScene();
        show(menu);
        ui.PartyMode party = new ui.PartyMode(audioManager);
        if (menu.getRoot() instanceof javafx.scene.layout.Pane root) {
            party.start(root);
        }
        // Long enough for the lights to spread out across the screen.
        after(3.5, () -> {
            capture("party.png");
            party.stop();
            audioManager.dispose();
            Platform.exit();
        });
    }

    /** Clicks Randomise if the screen offers it, otherwise leaves the board as it is. */
    private void placeFleetThroughTheUi(Scene scene) {
        for (Node node : scene.getRoot().lookupAll(".menu-button")) {
            if (node instanceof Button button && button.getText() != null
                    && button.getText().toLowerCase().contains("random")) {
                button.fire();
                return;
            }
        }
    }

    private List<Button> enemyTiles(Scene scene) {
        // Both grids share the tile class; the enemy grid is the second block of a hundred.
        List<Button> all = new ArrayList<>();
        for (Node node : scene.getRoot().lookupAll(".grid-tile")) {
            if (node instanceof Button button) {
                all.add(button);
            }
        }
        return all.size() > 100 ? all.subList(all.size() - 100, all.size()) : all;
    }

    private void show(Scene scene) {
        ui.UiFactory.applyTheme(scene);
        stage.setScene(scene);
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    private void capture(String name) {
        Scene scene = stage.getScene();
        WritableImage image = scene.snapshot(null);
        File file = new File(OUTPUT, name);
        try {
            ImageIO.write(toBufferedImage(image), "png", file);
            System.out.println("wrote " + file.getPath() + "  " + (int) image.getWidth() + "x" + (int) image.getHeight());
        } catch (Exception exception) {
            System.err.println("could not write " + file + ": " + exception);
        }
    }

    /** JavaFX has no PNG writer of its own, and pulling in javafx.swing just for this is not worth it. */
    private static BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private static void after(double seconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
        pause.setOnFinished(event -> action.run());
        pause.play();
    }

    private static Runnable noop() {
        return () -> { };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
