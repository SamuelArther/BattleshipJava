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
import javafx.scene.input.KeyCode;
import network.NetworkGameSession;
import ui.EndScene;
import ui.GameScene;
import ui.MainMenuScene;
import ui.SetupScene;
import ui.PartyMode;
import ui.SettingsScene;
import ui.StatsScene;
import ui.TermsScene;
import ui.UiFactory;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import settings.Settings;
import ui.UiFactory;

public class Main extends Application {
    private Stage stage;
    private AudioManager audioManager;
    private SetupScene activeSetupScene;
    private Board localPlayer1Board;
    private boolean stageIsBorderless;
    private PartyMode partyMode;

    // Ten keys in a fixed order. Nothing about ordinary play walks into this by accident.
    private static final KeyCode[] PARTY_CODE = {
        KeyCode.UP, KeyCode.UP, KeyCode.DOWN, KeyCode.DOWN,
        KeyCode.LEFT, KeyCode.RIGHT, KeyCode.LEFT, KeyCode.RIGHT,
        KeyCode.B, KeyCode.A
    };
    private int partyCodeProgress;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        audioManager = new AudioManager();
        partyMode = new PartyMode(audioManager);
        configureStage(stage);
        if (Settings.get().getAcceptedTermsVersion() < TermsScene.TERMS_VERSION) {
            showTerms();
        } else {
            showMainMenu();
        }
        applyDisplaySettings();
        stage.show();
    }

    private void configureStage(Stage target) {
        target.setTitle("Battleship");
        target.setMinWidth(1100);
        target.setMinHeight(760);
        target.setFullScreenExitHint("Press F11 or Esc to leave fullscreen");
        loadWindowIcon(target);
    }

    /**
     * Puts the window into whatever the player chose in Settings.
     *
     * Borderless needs an undecorated stage, and JavaFX only lets a stage's style be set
     * before it is first shown, so switching in or out of borderless builds a fresh stage and
     * moves the current scene across.
     */
    private void applyDisplaySettings() {
        Settings settings = Settings.get();
        boolean wantBorderless = settings.getDisplayMode() == Settings.DisplayMode.BORDERLESS;
        if (wantBorderless != stageIsBorderless) {
            rebuildStage(wantBorderless);
        }
        switch (settings.getDisplayMode()) {
            case WINDOWED -> {
                stage.setFullScreen(false);
                stage.setWidth(settings.getWindowWidth());
                stage.setHeight(settings.getWindowHeight());
                stage.centerOnScreen();
            }
            case BORDERLESS -> {
                stage.setFullScreen(false);
                Rectangle2D bounds = Screen.getPrimary().getBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
            }
            case FULLSCREEN -> stage.setFullScreen(true);
        }
    }

    private void rebuildStage(boolean borderless) {
        Stage replacement = new Stage();
        if (borderless) {
            replacement.initStyle(StageStyle.UNDECORATED);
        }
        configureStage(replacement);
        Stage previous = stage;
        Scene scene = previous.getScene();
        if (scene != null) {
            previous.setScene(null);
            replacement.setScene(scene);
        }
        stage = replacement;
        stageIsBorderless = borderless;
        replacement.show();
        previous.close();
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
            this::showStats,
            this::showSettings,
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
        audioManager.playInternet();

        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initOwner(stage);
        popup.initModality(javafx.stage.Modality.WINDOW_MODAL);
        popup.setTitle("No Network");
        popup.setWidth(460);
        popup.setHeight(210);
        popup.setResizable(false);

        javafx.scene.control.Label header = new javafx.scene.control.Label("No Network Connection");
        header.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 18));
        header.setTextFill(javafx.scene.paint.Color.web("#f4fbff"));

        javafx.scene.control.Label msg = new javafx.scene.control.Label(
                "Network multiplayer requires an active connection.\nPlease connect to a network and try again.");
        msg.setFont(javafx.scene.text.Font.font("Segoe UI", 14));
        msg.setTextFill(javafx.scene.paint.Color.web("#d7e9ff"));
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button okBtn = new Button("OK");
        okBtn.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        okBtn.setMinWidth(120);
        okBtn.setDefaultButton(true);
        okBtn.getStyleClass().add("primary-button");
        okBtn.setOnAction(e -> popup.close());

        VBox root = new VBox(16, header, msg, okBtn);
        root.setPadding(new javafx.geometry.Insets(28));
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.getStyleClass().add("alert-panel");

        Scene popupScene = new Scene(root);
        UiFactory.applyTheme(popupScene);
        popup.setScene(popupScene);
        popup.showAndWait();
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
        UiFactory.applyTheme(scene);
        // F11 toggles fullscreen and Esc leaves it. Nothing forces the window back on its own.
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
            trackPartyCode(event.getCode(), scene);
        });
        stage.setScene(scene);
    }

    /**
     * Watches for the code. A wrong key resets the run, except when that key is itself the
     * start of the code, which lets a fumbled attempt roll straight into the next one.
     */
    private void trackPartyCode(KeyCode pressed, Scene scene) {
        if (pressed == PARTY_CODE[partyCodeProgress]) {
            partyCodeProgress++;
            if (partyCodeProgress == PARTY_CODE.length) {
                partyCodeProgress = 0;
                toggleParty(scene);
            }
            return;
        }
        partyCodeProgress = pressed == PARTY_CODE[0] ? 1 : 0;
    }

    /** Enter the code again to send everyone home early. */
    private void toggleParty(Scene scene) {
        if (partyMode.isRunning()) {
            partyMode.stop();
        } else if (scene.getRoot() instanceof Pane root) {
            partyMode.start(root);
        }
    }

    private void showTerms() {
        setScene(new TermsScene(audioManager,
            () -> {
                Settings.get().acceptTerms(TermsScene.TERMS_VERSION);
                showMainMenu();
            },
            Platform::exit).createScene());
    }

    private void showStats() {
        setScene(new StatsScene(audioManager, this::showMainMenu).createScene());
    }

    private void showSettings() {
        setScene(new SettingsScene(audioManager, this::showMainMenu, this::applyDisplaySettings).createScene());
    }

    private void loadWindowIcon(Stage target) {
        try {
            java.net.URL iconUrl = getClass().getResource("/icon/icon.jpg");
            if (iconUrl != null) {
                target.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (RuntimeException ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
