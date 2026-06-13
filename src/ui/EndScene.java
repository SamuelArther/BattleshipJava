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
import settings.Achievements;
import settings.Settings;

import java.util.List;

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

        Label title = UiFactory.createScreenTitle(titleText);

        Label message = new Label(messageText);
        message.setTextFill(Color.web("#d7e9ff"));
        message.setFont(Font.font("Georgia", 20));
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);

        Runnable mainMenuAction = () -> {
            audioManager.stopEndMusic();
            menuAction.run();
        };
        Runnable closeAction = () -> {
            audioManager.stopEndMusic();
            exitAction.run();
        };

        VBox content = new VBox(18, title, message);
        buildUnlockedPanel().ifPresent(content.getChildren()::add);
        content.getChildren().addAll(
            UiFactory.createMenuButton("Main Menu", audioManager, mainMenuAction),
            UiFactory.createMenuButton("Exit", audioManager, closeAction));
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20, 0, 40, 0));
        content.setMaxWidth(700);

        root.getChildren().add(content);
        return new Scene(root, Settings.get().getWindowWidth(), Settings.get().getWindowHeight());
    }

    /** Anything the game just earned. Draining the list here means it is shown exactly once. */
    private java.util.Optional<VBox> buildUnlockedPanel() {
        List<Achievements.Achievement> unlocked = Achievements.get().takeRecentlyUnlocked();
        if (unlocked.isEmpty()) {
            return java.util.Optional.empty();
        }
        VBox panel = new VBox(8);
        Label heading = new Label(unlocked.size() == 1 ? "Achievement unlocked" : "Achievements unlocked");
        heading.getStyleClass().add("section-title");
        panel.getChildren().add(heading);
        for (Achievements.Achievement achievement : unlocked) {
            Label label = new Label(achievement.getDisplayName() + " — " + achievement.getDescription());
            label.getStyleClass().add("achievement-unlocked");
            label.setWrapText(true);
            panel.getChildren().add(label);
        }
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(16, 22, 18, 22));
        panel.setMaxWidth(620);
        panel.setAlignment(Pos.CENTER);
        return java.util.Optional.of(panel);
    }
}
