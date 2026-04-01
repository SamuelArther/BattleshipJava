import ai.Difficulty;
import audio.AudioManager;
import game.Board;
import game.GameMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.input.KeyCombination;
import network.NetworkGameSession;
import ui.EndScene;
import ui.GameScene;
import ui.MainMenuScene;
import ui.SetupScene;
import ui.UiFactory;

public class Main extends Application {
    private Stage stage;
    private AudioManager audioManager;
    private SetupScene activeSetupScene;
    private Board localPlayer1Board;

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
        audioManager.stopEndMusic();
        audioManager.playMenuMusic();
        MainMenuScene mainMenuScene = new MainMenuScene(
            audioManager,
            () -> showSetup(GameMode.SINGLEPLAYER),
            () -> showLocalSetup(1),
            () -> showNetworkSetupIfOnline(GameMode.HOST),
            () -> showNetworkSetupIfOnline(GameMode.JOIN),
            Platform::exit
        );
        setScene(mainMenuScene.createScene());
    }

    private void showNetworkSetupIfOnline(GameMode gameMode) {
        new Thread(() -> {
            boolean online;
            try {
                java.net.InetAddress.getByName("8.8.8.8").isReachable(2000);
                online = true;
            } catch (Exception e) {
                // fallback: check if we have any non-loopback network interface up
                online = false;
                try {
                    java.util.Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
                    while (ifaces.hasMoreElements()) {
                        java.net.NetworkInterface iface = ifaces.nextElement();
                        if (iface.isUp() && !iface.isLoopback()) {
                            online = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            boolean isOnline = online;
            Platform.runLater(() -> {
                if (isOnline) {
                    showSetup(gameMode);
                } else {
                    showOfflinePopup();
                }
            });
        }, "battleship-connectivity-check").start();
    }

    private void showOfflinePopup() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("No Network");
        alert.setHeaderText("Please connect to the internet.");
        alert.setContentText("Network multiplayer requires an active network connection. Connect to a network and try again.");
        alert.initOwner(stage);
        alert.showAndWait();
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

    private void showLocalSetup(int playerNum) {
        activeSetupScene = new SetupScene(
            audioManager,
            GameMode.LOCAL,
            playerNum,
            this::showMainMenu,
            (board, ignored) -> onLocalPlayerReady(playerNum, board),
            null
        );
        setScene(activeSetupScene.createScene());
    }

    private void onLocalPlayerReady(int playerNum, Board board) {
        activeSetupScene = null;
        if (playerNum == 1) {
            localPlayer1Board = board;
            showLocalHandoff();
        } else {
            startLocalGame(localPlayer1Board, board);
        }
    }

    private void showLocalHandoff() {
        Pane root = UiFactory.createRootPane();

        Label title = new Label("Player 2's Setup");
        title.setTextFill(Color.web("#f4fbff"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 40));

        Label msg = new Label(
            "Pass the computer to Player 2.\nPlayer 1's ships are now hidden.\nPress Continue when Player 2 is ready."
        );
        msg.setTextFill(Color.web("#d7e9ff"));
        msg.setFont(Font.font("Georgia", 20));
        msg.setTextAlignment(TextAlignment.CENTER);

        Button continueBtn = UiFactory.createMenuButton("Continue", audioManager, () -> showLocalSetup(2));
        continueBtn.setPrefWidth(220);
        continueBtn.setPrefHeight(52);

        VBox box = new VBox(30, title, msg, continueBtn);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);

        setScene(new Scene(root, 1100, 760));
    }

    private void startLocalGame(Board board1, Board board2) {
        audioManager.stopMenuMusic();
        GameScene gameScene = GameScene.createLocal(audioManager, board1, board2, this::showEndScene, this::showMainMenu);
        setScene(gameScene.createScene());
    }

    private void showEndScene(String title, String message) {
        if ("Victory".equalsIgnoreCase(title)) {
            audioManager.playVictoryMusic();
        } else if ("Defeat".equalsIgnoreCase(title) || "Loss".equalsIgnoreCase(title)) {
            audioManager.playLossMusic();
        } else {
            audioManager.stopEndMusic();
        }
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
