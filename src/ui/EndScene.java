package ui;

import audio.AudioManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.Insets;

public class EndScene {
    private final AudioManager audioManager;
    private final String titleText;
    private final String messageText;
    private final Runnable menuAction;
    private final Runnable exitAction;

    public EndScene(AudioManager audioManager, String titleText, String messageText, Runnable menuAction, Runnable exitAction) {
        this.audioManager = audioManager;
        this.titleText = titleText;
        this.messageText = messageText;
        this.menuAction = menuAction;
        this.exitAction = exitAction;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        Label title = new Label(titleText);
        title.setTextFill(Color.web("#f4fbff"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 36));

        Label message = new Label(messageText);
        message.setTextFill(Color.web("#d7e9ff"));
        message.setFont(Font.font("Georgia", 20));
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);

        VBox content = new VBox(18,
            title,
            message,
            UiFactory.createMenuButton("Main Menu", audioManager, menuAction),
            UiFactory.createMenuButton("Exit", audioManager, exitAction)
        );
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20, 0, 40, 0));
        content.setMaxWidth(700);

        root.getChildren().add(content);
        return new Scene(root, 1100, 760);
    }
}
