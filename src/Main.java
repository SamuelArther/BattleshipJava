import ai.Difficulty;
import audio.AudioManager;
import game.Board;
import game.GameMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.input.KeyCombination;
import network.NetworkGameSession;
import ui.EndScene;
import ui.GameScene;
import ui.MainMenuScene;
import ui.SetupScene;

public class Main extends Application {
    private Stage stage;
    private AudioManager audioManager;
    private SetupScene activeSetupScene;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);
        audioManager = new AudioManager();
        stage.setTitle("Battleship");
        stage.setWidth(1600);
        stage.setHeight(900);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setResizable(true);
        stage.setAlwaysOnTop(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        loadWindowIcon();
        showMainMenu();
        stage.show();
        stage.setFullScreen(true);
        stage.toFront();
        stage.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                enforceFullscreen();
            }
        });
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
        stage.setScene(scene);
        Platform.runLater(this::enforceFullscreen);
    }

    private void enforceFullscreen() {
        if (stage == null) {
            return;
        }
        try {
            stage.setAlwaysOnTop(true);
            stage.setFullScreen(true);
            stage.toFront();
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                stage.getScene().getRoot().requestFocus();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void loadWindowIcon() {
        try {
            java.net.URL iconUrl = getClass().getResource("/icon/icon.jpg");
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
