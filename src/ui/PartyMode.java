package ui;

import audio.AudioManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The disco.
 *
 * Lays a layer of coloured lights over whatever screen is showing, sweeps them around in time
 * with the track, and when the music runs out fades the whole thing away and takes itself back
 * off the scene so the game is exactly as it was.
 *
 * The overlay never accepts the mouse, so a game underneath stays playable throughout.
 */
public final class PartyMode {

    private static final Duration FADE_OUT = Duration.seconds(3);
    /** Used only if the track will not load, so the lights still end by themselves. */
    private static final Duration FALLBACK_LENGTH = Duration.seconds(20);

    private static final Color[] LIGHT_COLOURS = {
        Color.web("#ff2d95"), Color.web("#00e5ff"), Color.web("#ffd200"),
        Color.web("#7cff4f"), Color.web("#b14bff"), Color.web("#ff6a00")
    };

    private final AudioManager audioManager;
    private final Random random = new Random();

    private Pane host;
    private Pane overlay;
    private Timeline sweep;
    private boolean running;

    public PartyMode(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public boolean isRunning() {
        return running;
    }

    /** Starts the party over the given root, if one is not already going. */
    public void start(Pane root) {
        if (running || root == null) {
            return;
        }
        running = true;
        host = root;

        overlay = buildOverlay();
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().add(overlay);

        sweep.play();
        audioManager.playParty(this::finish);
        if (!audioManager.isPartyPlaying()) {
            // No audio available, so end the lights on a timer instead of on the track.
            Timeline fallback = new Timeline(new KeyFrame(FALLBACK_LENGTH, event -> finish()));
            fallback.play();
        }
    }

    /** Ends the party early. The lights still fade rather than snapping off. */
    public void stop() {
        if (running) {
            audioManager.stopParty();
            finish();
        }
    }

    private Pane buildOverlay() {
        StackPane layer = new StackPane();
        layer.setMouseTransparent(true);
        layer.setPickOnBounds(false);

        List<Circle> lights = new ArrayList<>();
        for (int i = 0; i < LIGHT_COLOURS.length; i++) {
            Circle light = new Circle(150 + random.nextInt(90));
            light.setFill(LIGHT_COLOURS[i]);
            light.setOpacity(0.34);
            light.setEffect(new GaussianBlur(90));
            light.setBlendMode(BlendMode.SCREEN);
            lights.add(light);
            layer.getChildren().add(light);
        }

        sweep = new Timeline();
        sweep.setCycleCount(Timeline.INDEFINITE);
        for (int i = 0; i < lights.size(); i++) {
            Circle light = lights.get(i);
            double phase = 0.45 + (i * 0.13);
            // Each light runs its own loop, so they drift apart instead of moving as one block.
            for (int step = 1; step <= 8; step++) {
                sweep.getKeyFrames().add(new KeyFrame(Duration.seconds(phase * step),
                    new KeyValue(light.translateXProperty(), (random.nextDouble() - 0.5) * 900, Interpolator.EASE_BOTH),
                    new KeyValue(light.translateYProperty(), (random.nextDouble() - 0.5) * 560, Interpolator.EASE_BOTH),
                    new KeyValue(light.opacityProperty(), 0.18 + random.nextDouble() * 0.30, Interpolator.EASE_BOTH),
                    new KeyValue(light.scaleXProperty(), 0.7 + random.nextDouble() * 0.8, Interpolator.EASE_BOTH),
                    new KeyValue(light.scaleYProperty(), 0.7 + random.nextDouble() * 0.8, Interpolator.EASE_BOTH)));
            }
        }
        return layer;
    }

    private void finish() {
        if (!running) {
            return;
        }
        running = false;

        FadeTransition fade = new FadeTransition(FADE_OUT, overlay);
        fade.setFromValue(overlay.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            if (sweep != null) {
                sweep.stop();
            }
            if (host != null && overlay != null) {
                host.getChildren().remove(overlay);
            }
            overlay = null;
            host = null;
        });
        fade.play();
    }
}
