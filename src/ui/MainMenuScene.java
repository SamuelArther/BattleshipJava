package ui;

import audio.AudioManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.net.URL;

public class MainMenuScene {
    private final AudioManager audioManager;
    private final Runnable playAiAction;
    private final Runnable localAction;
    private final Runnable hostAction;
    private final Runnable joinAction;
    private final Runnable exitAction;

    public MainMenuScene(AudioManager audioManager, Runnable playAiAction, Runnable localAction, Runnable hostAction, Runnable joinAction, Runnable exitAction) {
        this.audioManager = audioManager;
        this.playAiAction = playAiAction;
        this.localAction = localAction;
        this.hostAction = hostAction;
        this.joinAction = joinAction;
        this.exitAction = exitAction;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        ImageView shipGraphic = createShipGraphic();

        Label title = new Label("Battleship");
        title.setTextFill(Color.web("#f4fbff"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 40));

        VBox brandBox = new VBox(6, shipGraphic, title);
        brandBox.setAlignment(Pos.CENTER);

        VBox content = new VBox(18,
            brandBox,
            UiFactory.createMenuButton("Play vs AI", audioManager, playAiAction),
            UiFactory.createMenuButton("Local Multiplayer", audioManager, localAction),
            UiFactory.createMenuButton("Host Game", audioManager, hostAction),
            UiFactory.createMenuButton("Join Game", audioManager, joinAction),
            UiFactory.createMenuButton("Quit Battleship", audioManager, exitAction)
        );
        content.setAlignment(Pos.CENTER);

        root.getChildren().add(content);
        return new Scene(root, 1100, 760);
    }

    private ImageView createShipGraphic() {
        Image image = loadShipImage();
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(260);
        if (image != null) {
            double cropY = image.getHeight() * 0.30;
            double cropHeight = image.getHeight() * 0.26;
            imageView.setViewport(new Rectangle2D(0, cropY, image.getWidth(), cropHeight));
            imageView.setImage(image);
            imageView.setEffect(new Blend(
                BlendMode.SRC_ATOP,
                null,
                new ColorInput(0, 0, 260, 120, Color.web("#f4fbff"))
            ));
        }
        return imageView;
    }

    private Image loadShipImage() {
        try {
            URL resource = getClass().getResource("/ship/ship.png");
            if (resource != null) {
                return new Image(resource.toExternalForm());
            }
        } catch (RuntimeException ignored) {
        }

        File fallback = new File("resources/ship/ship.png");
        if (fallback.exists()) {
            try {
                return new Image(fallback.toURI().toString());
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }
}
