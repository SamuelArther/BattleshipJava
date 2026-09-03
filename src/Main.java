import ai.Difficulty;
import audio.AudioManager;
import game.Board;
import game.GameMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import network.NetworkGameSession;
import ui.EndScene;
import ui.GameScene;
import ui.MainMenuScene;
import ui.SetupScene;

import java.net.URL;

public class Main extends Application {
    private Stage stage;
    private AudioManager audioManager;
    private SetupScene activeSetupScene;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        audioManager = new AudioManager();
        stage.setTitle("Battleship");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.setFullScreenExitHint("Press F11 or Esc to leave fullscreen");
        loadWindowIcon();
        showMainMenu();
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() {
        if (activeSetupScene != null) {
            activeSetupScene.shutdown();
        }
        if (audioManager != null) {
            audioManager.dispose();
        }
    }

    private void showMainMenu() {
        if (activeSetupScene != null) {
            activeSetupScene.shutdown();
            activeSetupScene = null;
        }
        audioManager.playMenuMusic();
        MainMenuScene mainMenuScene = new MainMenuScene(
            audioManager,
            () -> showSetup(GameMode.SINGLEPLAYER),
            () -> showSetup(GameMode.HOST),
            () -> showSetup(GameMode.JOIN),
            Platform::exit
        );
        setScene(mainMenuScene.createScene());
    }

    private void showSetup(GameMode gameMode) {
        activeSetupScene = new SetupScene(
            audioManager,
            gameMode,
            this::showMainMenu,
            this::startSinglePlayerGame,
            this::startNetworkGame
        );
        setScene(activeSetupScene.createScene());
    }

    private void startSinglePlayerGame(Board board, Difficulty difficulty) {
        activeSetupScene = null;
        audioManager.stopMenuMusic();
        GameScene gameScene = GameScene.createSinglePlayer(audioManager, board, difficulty, this::showEndScene, this::showMainMenu);
        setScene(gameScene.createScene());
    }

    private void startNetworkGame(Board board, NetworkGameSession session, boolean myTurn) {
        activeSetupScene = null;
        audioManager.stopMenuMusic();
        GameScene gameScene = GameScene.createNetwork(audioManager, board, session, myTurn, this::showEndScene, this::showMainMenu);
        setScene(gameScene.createScene());
    }

    private void showEndScene(String title, String message) {
        EndScene endScene = new EndScene(audioManager, title, message, this::showMainMenu, Platform::exit);
        setScene(endScene.createScene());
    }

    private void setScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
        });
        stage.setScene(scene);
    }

    private void loadWindowIcon() {
        try {
            URL iconUrl = getClass().getResource("/icon/icon.jpg");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
