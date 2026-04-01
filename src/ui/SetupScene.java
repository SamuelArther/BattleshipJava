package ui;

import ai.Difficulty;
import audio.AudioManager;
import game.Board;
import game.GameMode;
import game.Orientation;
import game.ShipType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import network.LanDiscovery;
import network.NetworkGameSession;
import network.NetworkMessageListener;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;

public class SetupScene implements NetworkMessageListener {
    @FunctionalInterface
    public interface SinglePlayerStartHandler {
        void start(Board board, Difficulty difficulty);
    }

    @FunctionalInterface
    public interface NetworkStartHandler {
        void start(Board board, NetworkGameSession session, boolean myTurn);
    }

    private final AudioManager audioManager;
    private final GameMode gameMode;
    private final int playerNum;
    private final Runnable backAction;
    private final SinglePlayerStartHandler singlePlayerStartHandler;
    private final NetworkStartHandler networkStartHandler;
    private final Board board = new Board();
    private final Random random = new Random();
    private final Button[][] gridButtons = new Button[Board.SIZE][Board.SIZE];
    private final Map<ShipType, Button> shipButtons = new EnumMap<>(ShipType.class);

    private Orientation orientation = Orientation.HORIZONTAL;
    private ShipType selectedShip = ShipType.CARRIER;
    private Label statusLabel;
    private Label orientationLabel;
    private Label selectedShipLabel;
    private Label difficultyDescriptionLabel;
    private ComboBox<Difficulty> difficultyComboBox;
    private TextField codeField;
    private Button connectButton;
    private Button readyButton;
    private Label codeDisplayLabel;
    private boolean localReady;
    private NetworkGameSession networkSession;

    public SetupScene(AudioManager audioManager, GameMode gameMode, Runnable backAction, SinglePlayerStartHandler singlePlayerStartHandler, NetworkStartHandler networkStartHandler) {
        this(audioManager, gameMode, 1, backAction, singlePlayerStartHandler, networkStartHandler);
    }

    public SetupScene(AudioManager audioManager, GameMode gameMode, int playerNum, Runnable backAction, SinglePlayerStartHandler singlePlayerStartHandler, NetworkStartHandler networkStartHandler) {
        this.audioManager = audioManager;
        this.gameMode = gameMode;
        this.playerNum = playerNum;
        this.backAction = backAction;
        this.singlePlayerStartHandler = singlePlayerStartHandler;
        this.networkStartHandler = networkStartHandler;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        BorderPane layout = new BorderPane();
        layout.setMaxWidth(1000);
        layout.setPadding(new Insets(10));

        Label title = new Label(getTitleText());
        title.setTextFill(Color.web("#f4fbff"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 30));

        statusLabel = new Label(getStartingStatusText());
        statusLabel.setTextFill(Color.web("#d7e9ff"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(900);

        VBox topBox = new VBox(10, title, buildModeHeader(), statusLabel);
        topBox.setPadding(new Insets(0, 0, 18, 0));

        GridPane placementGrid = new GridPane();
        placementGrid.setHgap(3);
        placementGrid.setVgap(3);
        placementGrid.setAlignment(Pos.CENTER);
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button tileButton = UiFactory.createGridButton();
                final int finalX = x;
                final int finalY = y;
                tileButton.setOnAction(event -> handlePlacement(finalX, finalY));
                gridButtons[y][x] = tileButton;
                placementGrid.add(tileButton, x, y);
            }
        }

        VBox placementSection = UiFactory.createBoardSection(
            "Placement Grid",
            placementGrid,
            UiFactory.createLegend(
                "#90caf9|Placed ship tile",
                "#b9d7ea|Open water"
            )
        );

        HBox centerBox = new HBox(28, placementSection, buildSidePanel());
        centerBox.setAlignment(Pos.TOP_CENTER);

        layout.setTop(topBox);
        layout.setCenter(centerBox);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 1100, 760);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.R) {
                rotateShip();
            }
        });

        updateBoardButtons();
        updateShipButtons();
        startNetworkIfNeeded();
        return scene;
    }

    public void shutdown() {
        if (networkSession != null) {
            networkSession.close();
        }
    }

    @Override
    public void onWaitingForOpponent(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void onConnected() {
        statusLabel.setText("Connected. Place all ships and press Ready.");
        if (connectButton != null) {
            connectButton.setDisable(true);
        }
    }

    @Override
    public void onPlaceShips() {
        statusLabel.setText("Connection established. Place all ships and press Ready.");
    }

    @Override
    public void onRemoteReady() {
        statusLabel.setText(localReady ? "Opponent is ready. Starting game..." : "Opponent is ready. Finish placing ships and press Ready.");
    }

    @Override
    public void onGameStart(boolean myTurn) {
        networkStartHandler.start(board, networkSession, myTurn);
    }

    @Override
    public void onDisconnected(String message) {
        statusLabel.setText(message);
        localReady = false;
        if (readyButton != null) {
            readyButton.setDisable(false);
        }
        if (connectButton != null && gameMode == GameMode.JOIN) {
            connectButton.setDisable(false);
        }
    }

    @Override
    public void onError(String message) {
        if (gameMode == GameMode.HOST || gameMode == GameMode.JOIN) {
            audioManager.playInternet();
        }
        statusLabel.setText(message);
        localReady = false;
        if (connectButton != null && gameMode == GameMode.JOIN) {
            connectButton.setDisable(false);
        }
    }

    private VBox buildSidePanel() {
        Label shipsLabel = new Label("Fleet");
        shipsLabel.setTextFill(Color.web("#f4fbff"));
        shipsLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 22));

        VBox shipsBox = new VBox(10);
        for (ShipType shipType : ShipType.values()) {
            Button button = UiFactory.createMenuButton(shipType.getDisplayName() + " (" + shipType.getSize() + ")", audioManager, () -> selectShip(shipType));
            button.setPrefWidth(260);
            shipButtons.put(shipType, button);
            shipsBox.getChildren().add(button);
        }

        selectedShipLabel = new Label();
        selectedShipLabel.setTextFill(Color.web("#f4fbff"));
        selectedShipLabel.setWrapText(true);

        orientationLabel = new Label();
        orientationLabel.setTextFill(Color.web("#d7e9ff"));
        updateSelectedShipLabel();
        updateOrientationLabel();

        Button rotateButton = UiFactory.createMenuButton("Rotate (R)", audioManager, this::rotateShip);
        Button randomButton = UiFactory.createMenuButton("Randomize", audioManager, this::randomizeBoard);
        Button clearButton = UiFactory.createMenuButton("Clear Board", audioManager, this::clearBoard);
        readyButton = UiFactory.createMenuButton(gameMode == GameMode.SINGLEPLAYER ? "Start Game" : gameMode == GameMode.LOCAL ? "Done" : "Ready", audioManager, this::handleReady);
        Button backButton = UiFactory.createMenuButton("Back", audioManager, () -> {
            shutdown();
            backAction.run();
        });

        VBox sidePanel = new VBox(14, shipsLabel, shipsBox, selectedShipLabel, orientationLabel, rotateButton, randomButton, clearButton);

        if (gameMode == GameMode.SINGLEPLAYER) {
            Label difficultyLabel = new Label("AI Difficulty");
            difficultyLabel.setTextFill(Color.web("#f4fbff"));
            difficultyLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
            difficultyComboBox = new ComboBox<>();
            difficultyComboBox.getItems().addAll(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.NIGHTMARE, Difficulty.US_NAVY);
            difficultyComboBox.getSelectionModel().select(Difficulty.MEDIUM);
            difficultyComboBox.setPrefWidth(260);
            difficultyDescriptionLabel = new Label();
            difficultyDescriptionLabel.setTextFill(Color.web("#d7e9ff"));
            difficultyDescriptionLabel.setWrapText(true);
            difficultyDescriptionLabel.setMaxWidth(260);
            updateDifficultyDescription();
            difficultyComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateDifficultyDescription());
            sidePanel.getChildren().addAll(difficultyLabel, difficultyComboBox, difficultyDescriptionLabel);
        }

        sidePanel.getChildren().addAll(readyButton, backButton);
        sidePanel.setAlignment(Pos.TOP_CENTER);
        return sidePanel;
    }

    private Node buildModeHeader() {
        if (gameMode == GameMode.LOCAL) {
            Label helper = new Label("Player " + playerNum + ": select a ship, place it on the grid. Press Done when finished.");
            helper.setTextFill(Color.web("#d7e9ff"));
            return helper;
        }
        if (gameMode == GameMode.HOST) {
            Label helpText = new Label("Share this code with your opponent:");
            helpText.setTextFill(Color.web("#d7e9ff"));
            codeDisplayLabel = new Label("Generating...");
            codeDisplayLabel.setTextFill(Color.web("#ffd166"));
            codeDisplayLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 32));
            VBox hostBox = new VBox(2, helpText, codeDisplayLabel);
            return hostBox;
        }
        if (gameMode == GameMode.JOIN) {
            Label joinLabel = new Label("Game Code");
            joinLabel.setTextFill(Color.web("#f4fbff"));
            codeField = new TextField();
            codeField.setPromptText("ABCDEF");
            codeField.setPrefWidth(130);
            codeField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-transform: uppercase;");
            codeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    String upper = newVal.toUpperCase().replaceAll("[^A-Z0-9]", "");
                    if (upper.length() > 6) upper = upper.substring(0, 6);
                    if (!upper.equals(newVal)) {
                        codeField.setText(upper);
                    }
                }
            });
            connectButton = UiFactory.createMenuButton("Connect", audioManager, this::connectWithCode);
            HBox joinBox = new HBox(12, joinLabel, codeField, connectButton);
            joinBox.setAlignment(Pos.CENTER_LEFT);
            return joinBox;
        }
        Label helper = new Label("Place your fleet. Click a ship, place it on the grid, and press Start Game.");
        helper.setTextFill(Color.web("#d7e9ff"));
        return helper;
    }

    private void startNetworkIfNeeded() {
        if (gameMode == GameMode.LOCAL) {
            return;
        }
        if (gameMode == GameMode.HOST) {
            networkSession = new NetworkGameSession(this);
            networkSession.host(NetworkGameSession.DEFAULT_PORT);
            if (codeDisplayLabel != null) {
                codeDisplayLabel.setText(networkSession.getJoinCode());
            }
        }
    }

    private void connectWithCode() {
        String code = codeField == null ? "" : codeField.getText().trim();
        if (code.length() < LanDiscovery.CODE_LENGTH) {
            statusLabel.setText("Enter the full " + LanDiscovery.CODE_LENGTH + "-character code from the host.");
            return;
        }
        if (networkSession != null) {
            networkSession.close();
        }
        networkSession = new NetworkGameSession(this);
        connectButton.setDisable(true);
        networkSession.joinWithCode(code);
    }

    private void handlePlacement(int x, int y) {
        if (localReady) {
            statusLabel.setText("You are already ready. Waiting for the other player.");
            return;
        }
        if (board.isShipPlaced(selectedShip)) {
            statusLabel.setText("That ship is already placed. Select another ship.");
            return;
        }
        if (board.placeShip(selectedShip, x, y, orientation)) {
            statusLabel.setText(selectedShip.getDisplayName() + " placed.");
            updateBoardButtons();
            updateShipButtons();
            selectNextAvailableShip();
        } else {
            statusLabel.setText("Invalid placement. Stay in bounds and avoid overlap.");
        }
    }

    private void selectShip(ShipType shipType) {
        selectedShip = shipType;
        updateShipButtons();
        updateSelectedShipLabel();
        statusLabel.setText("Selected " + shipType.getDisplayName() + ".");
    }

    private void rotateShip() {
        orientation = orientation.toggle();
        updateOrientationLabel();
        statusLabel.setText("Orientation set to " + orientation.name().toLowerCase() + ".");
    }

    private void randomizeBoard() {
        if (localReady) {
            return;
        }
        board.randomize(random);
        updateBoardButtons();
        updateShipButtons();
        selectNextAvailableShip();
        statusLabel.setText("Board randomized.");
    }

    private void clearBoard() {
        if (localReady) {
            return;
        }
        board.clear();
        selectedShip = ShipType.CARRIER;
        updateBoardButtons();
        updateShipButtons();
        updateSelectedShipLabel();
        updateOrientationLabel();
        statusLabel.setText("Board cleared.");
    }

    private void handleReady() {
        if (!board.allShipsPlaced()) {
            statusLabel.setText("Place all five ships before continuing.");
            return;
        }

        if (gameMode == GameMode.SINGLEPLAYER) {
            singlePlayerStartHandler.start(board, difficultyComboBox.getValue());
            return;
        }

        if (gameMode == GameMode.LOCAL) {
            singlePlayerStartHandler.start(board, null);
            return;
        }

        if (networkSession == null || !networkSession.isConnected()) {
            statusLabel.setText(gameMode == GameMode.HOST ? "Waiting for a player to connect." : "Connect to a host before pressing Ready.");
            return;
        }

        if (localReady) {
            statusLabel.setText("You are already ready. Waiting for the opponent.");
            return;
        }

        localReady = true;
        readyButton.setDisable(true);
        statusLabel.setText("Ready sent. Waiting for the opponent.");
        networkSession.sendReady();
    }

    private void updateDifficultyDescription() {
        if (difficultyComboBox != null && difficultyDescriptionLabel != null && difficultyComboBox.getValue() != null) {
            difficultyDescriptionLabel.setText(difficultyComboBox.getValue().getDescription());
        }
    }

    private void updateSelectedShipLabel() {
        if (selectedShipLabel != null) {
            selectedShipLabel.setText("Selected ship: " + selectedShip.getDisplayName() + " (" + selectedShip.getSize() + " tiles)");
        }
    }

    private void updateBoardButtons() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = gridButtons[y][x];
                if (board.getTile(x, y).hasShip()) {
                    UiFactory.styleGridButton(button, "#90caf9", board.getTile(x, y).getShip().getType().name().substring(0, 1));
                } else {
                    UiFactory.styleGridButton(button, "#b9d7ea", "");
                }
            }
        }
    }

    private void updateShipButtons() {
        for (ShipType shipType : ShipType.values()) {
            Button button = shipButtons.get(shipType);
            boolean placed = board.isShipPlaced(shipType);
            button.setDisable(placed);
            if (shipType == selectedShip && !placed) {
                button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #ffd166; -fx-text-fill: #0b1f33;");
            } else if (placed) {
                button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #6fb98f; -fx-text-fill: #0b1f33;");
            } else {
                button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #d7e9ff; -fx-text-fill: #0b1f33;");
            }
        }
    }

    private void updateOrientationLabel() {
        if (orientationLabel != null) {
            orientationLabel.setText("Orientation: " + orientation.name().charAt(0) + orientation.name().substring(1).toLowerCase());
        }
    }

    private void selectNextAvailableShip() {
        for (ShipType shipType : ShipType.values()) {
            if (!board.isShipPlaced(shipType)) {
                selectedShip = shipType;
                updateSelectedShipLabel();
                updateShipButtons();
                return;
            }
        }
        updateSelectedShipLabel();
        updateShipButtons();
    }

    private String getTitleText() {
        return switch (gameMode) {
            case SINGLEPLAYER -> "Ship Placement";
            case HOST -> "Host Game Setup";
            case JOIN -> "Join Game Setup";
            case LOCAL -> "Player " + playerNum + " Ship Placement";
        };
    }

    private String getStartingStatusText() {
        return switch (gameMode) {
            case SINGLEPLAYER -> "Select a ship, place it on the grid, and press Start Game.";
            case HOST -> "Starting server. Share your IP with another player.";
            case JOIN -> "Enter the host IP, connect, then place your ships.";
            case LOCAL -> "Player " + playerNum + ": place all five ships, then press Done.";
        };
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return "127.0.0.1";
    }
}
