package ui;

import ai.BattleshipAI;
import settings.Achievements;
import settings.Statistics;
import ai.Difficulty;
import audio.AudioManager;
import game.AttackOutcome;
import game.AttackResult;
import game.Board;
import game.Coordinate;
import game.Ship;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import network.NetworkGameSession;
import network.NetworkMessageListener;

import java.io.File;
import java.net.URL;

public class GameScene implements NetworkMessageListener {
    @FunctionalInterface
    public interface FinishHandler {
        void finish(String title, String message);
    }

    private final AudioManager audioManager;
    private Board playerBoard;
    private final FinishHandler finishHandler;
    private final Runnable menuAction;
    private final Button[][] playerButtons = new Button[Board.SIZE][Board.SIZE];
    private final Button[][] enemyButtons = new Button[Board.SIZE][Board.SIZE];
    private boolean[][] enemyAttacked = new boolean[Board.SIZE][Board.SIZE];
    private boolean[][] enemyHits = new boolean[Board.SIZE][Board.SIZE];

    // Local multiplayer fields
    private Board player1Board;
    private Board player2Board;
    private boolean isLocalMultiplayer;
    private int currentPlayer = 1;
    private final boolean[][] p1EnemyAttacked = new boolean[Board.SIZE][Board.SIZE];
    private final boolean[][] p1EnemyHits = new boolean[Board.SIZE][Board.SIZE];
    private final boolean[][] p2EnemyAttacked = new boolean[Board.SIZE][Board.SIZE];
    private final boolean[][] p2EnemyHits = new boolean[Board.SIZE][Board.SIZE];

    private Label statusLabel;
    private Label playerSummaryLabel;
    private Label enemySummaryLabel;
    private Label targetLabel;
    private Button fireButton;
    private Button passTurnButton;
    private boolean myTurn;
    private boolean gameFinished;
    private boolean shotAnimationActive;
    private int selectedTargetX = -1;
    private int selectedTargetY = -1;
    private int pendingAttackX = -1;
    private int pendingAttackY = -1;

    private Board enemyBoard;
    private BattleshipAI ai;
    private Difficulty difficulty;
    // Counted for the single-player record only; see Statistics.
    private int playerShots;
    private int playerHits;
    // Every shot of the current game, newest last, shown by the Battle Log button.
    private final java.util.List<String> battleLog = new java.util.ArrayList<>();
    private NetworkGameSession networkSession;
    private StackPane root;
    private Pane effectsLayer;
    private Image bombImage;

    // Ballistic missile perk tracking (index 1=p1, 2=p2; index 1 for single player/network)
    private final int[] consecutiveSinks = new int[3];
    private final boolean[] ballisticEarned = new boolean[3];
    private Label ballisticLabel;
    private Button ballisticButton;
    private boolean ballisticMissileActive = false;

    // Ship visibility for local multiplayer
    private boolean shipsVisible = true;

    private GameScene(AudioManager audioManager, Board playerBoard, FinishHandler finishHandler, Runnable menuAction) {
        this.audioManager = audioManager;
        this.playerBoard = playerBoard;
        this.finishHandler = finishHandler;
        this.menuAction = menuAction;
    }

    public static GameScene createSinglePlayer(AudioManager audioManager, Board playerBoard, Difficulty difficulty, FinishHandler finishHandler, Runnable menuAction) {
        GameScene gameScene = new GameScene(audioManager, playerBoard, finishHandler, menuAction);
        gameScene.enemyBoard = new Board();
        gameScene.enemyBoard.randomize(new java.util.Random());
        gameScene.ai = new BattleshipAI(difficulty);
        gameScene.difficulty = difficulty;
        gameScene.myTurn = true;
        return gameScene;
    }

    public static GameScene createNetwork(AudioManager audioManager, Board playerBoard, NetworkGameSession networkSession, boolean myTurn, FinishHandler finishHandler, Runnable menuAction) {
        GameScene gameScene = new GameScene(audioManager, playerBoard, finishHandler, menuAction);
        gameScene.networkSession = networkSession;
        gameScene.myTurn = myTurn;
        networkSession.setListener(gameScene);
        return gameScene;
    }

    public static GameScene createLocal(AudioManager audioManager, Board board1, Board board2, FinishHandler finishHandler, Runnable menuAction) {
        GameScene gameScene = new GameScene(audioManager, board1, finishHandler, menuAction);
        gameScene.player1Board = board1;
        gameScene.player2Board = board2;
        gameScene.enemyBoard = board2;
        gameScene.isLocalMultiplayer = true;
        gameScene.currentPlayer = 1;
        gameScene.myTurn = true;
        gameScene.enemyAttacked = gameScene.p1EnemyAttacked;
        gameScene.enemyHits = gameScene.p1EnemyHits;
        return gameScene;
    }

    public Scene createScene() {
        root = (StackPane) UiFactory.createRootPane();
        effectsLayer = new Pane();
        effectsLayer.setMouseTransparent(true);
        bombImage = loadBombImage();
        BorderPane layout = new BorderPane();
        layout.setMaxWidth(1020);
        layout.setPadding(new Insets(10));

        Label title = new Label("Battle");
        title.setTextFill(Color.web("#f4fbff"));
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 30));

        statusLabel = new Label(isLocalMultiplayer ? "Player 1's turn." : (myTurn ? "Your turn." : "Opponent's turn."));
        statusLabel.setTextFill(Color.web("#d7e9ff"));
        statusLabel.setFont(Font.font("Georgia", 18));

        VBox topBox = new VBox(6, title, statusLabel);
        topBox.setPadding(new Insets(0, 0, 18, 0));

        playerSummaryLabel = createSummaryLabel();
        enemySummaryLabel = createSummaryLabel();

        ballisticLabel = new Label("BALLISTIC MISSILE EARNED!");
        ballisticLabel.setTextFill(Color.web("#ff0000"));
        ballisticLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        ballisticLabel.setVisible(false);

        VBox playerBox = UiFactory.createBoardSection(
            "Your Grid",
            buildPlayerGrid(),
            UiFactory.createLegend(
                UiFactory.entry(UiFactory.TileStyle.SHIP, "Your ship"),
                UiFactory.entry(UiFactory.TileStyle.HIT, "Hit"),
                UiFactory.entry(UiFactory.TileStyle.MISS, "Miss")
            )
        );
        playerBox.getChildren().add(playerSummaryLabel);
        playerBox.getChildren().add(ballisticLabel);

        ballisticButton = new Button("USE BALLISTIC MISSILE");
        ballisticButton.getStyleClass().add("danger-button");
        ballisticButton.setVisible(false);
        ballisticButton.setOnAction(e -> {
            audioManager.playSelect();
            ballisticMissileActive = true;
            ballisticButton.setDisable(true);
            ballisticButton.setText("BALLISTIC MISSILE ARMED");
        });
        playerBox.getChildren().add(ballisticButton);

        VBox enemyBox = UiFactory.createBoardSection(
            "Enemy Grid",
            buildEnemyGrid(),
            UiFactory.createLegend(
                UiFactory.entry(UiFactory.TileStyle.WATER, "Unfired tile"),
                UiFactory.entry(UiFactory.TileStyle.HIT, "Confirmed hit"),
                UiFactory.entry(UiFactory.TileStyle.MISS, "Confirmed miss")
            )
        );
        enemyBox.getChildren().add(enemySummaryLabel);

        targetLabel = new Label("Click an enemy square to fire.");
        targetLabel.setTextFill(Color.web("#f4fbff"));
        targetLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 16));

        fireButton = new Button("Fire!");
        fireButton.setPrefWidth(180);
        fireButton.setPrefHeight(48);
        fireButton.getStyleClass().add("danger-button");
        fireButton.setManaged(false);
        fireButton.setVisible(false);

        passTurnButton = new Button("Pass Turn");
        passTurnButton.setPrefWidth(180);
        passTurnButton.setPrefHeight(48);
        passTurnButton.getStyleClass().add("primary-button");
        passTurnButton.setOnAction(e -> passTurn());

        VBox attackBox = new VBox(8, targetLabel, fireButton, passTurnButton);
        attackBox.setAlignment(Pos.CENTER);
        enemyBox.getChildren().add(attackBox);

        HBox centerBox = new HBox(30, playerBox, enemyBox);
        centerBox.setAlignment(Pos.TOP_CENTER);

        Button menuButton = UiFactory.createMenuButton("Main Menu", audioManager, () -> {
            if (networkSession != null) {
                networkSession.close();
            }
            menuAction.run();
        });
        Button logButton = UiFactory.createMenuButton("Battle Log", audioManager, this::showBattleLog);

        javafx.scene.layout.HBox bottomButtons = new javafx.scene.layout.HBox(14, logButton, menuButton);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox bottomBox = new VBox(12, bottomButtons);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(8, 0, 8, 0));

        layout.setTop(topBox);
        layout.setCenter(centerBox);
        layout.setBottom(bottomBox);

        root.getChildren().addAll(layout, effectsLayer);
        refreshPlayerGrid();
        refreshEnemyGrid();
        updateSummaries();
        updateEnemyInteractivity();
        audioManager.playSirYesSir();
        return new Scene(root, 1100, 760);
    }

    @Override
    public void onTurnGranted() {
        if (gameFinished) {
            return;
        }
        myTurn = true;
        statusLabel.setText("Your turn.");
        clearSelectedTarget();
        updateEnemyInteractivity();
    }

    @Override
    public void onAttackReceived(int x, int y) {
        if (gameFinished) {
            return;
        }
        // Nothing stops a peer sending coordinates off the grid, so check before indexing it.
        if (!playerBoard.isInBounds(x, y)) {
            finishGame("Connection Closed", "An illegal network move was detected.");
            return;
        }
        animateShot(playerButtons[y][x], () -> {
            AttackOutcome outcome = playerBoard.receiveAttack(x, y);
            log("Your opponent", x, y, outcome);
            if (outcome.getResult() == AttackResult.ALREADY_ATTACKED || outcome.getResult() == AttackResult.INVALID) {
                finishGame("Connection Closed", "An illegal network move was detected.");
                return;
            }
            refreshPlayerGrid();
            updateSummaries();
            networkSession.sendResult(outcome.getResult() == AttackResult.HIT);

            if (outcome.getResult() == AttackResult.HIT) {
                audioManager.playExplosion();
            }

            if (outcome.isGameOver()) {
                networkSession.sendLose();
                finishGame("Defeat", "All of your ships were sunk.");
            } else {
                statusLabel.setText("Opponent fired at " + toGridRef(x, y) + ".");
            }
        });
    }

    @Override
    public void onAttackResult(boolean hit) {
        if (gameFinished || pendingAttackX < 0 || pendingAttackY < 0) {
            return;
        }
        enemyAttacked[pendingAttackY][pendingAttackX] = true;
        enemyHits[pendingAttackY][pendingAttackX] = hit;
        refreshEnemyGrid();
        if (hit) {
            statusLabel.setText("Hit confirmed at " + toGridRef(pendingAttackX, pendingAttackY) + ".");
            audioManager.playExplosion();
        } else {
            statusLabel.setText("Miss at " + toGridRef(pendingAttackX, pendingAttackY) + ".");
        }
        pendingAttackX = -1;
        pendingAttackY = -1;
        clearSelectedTarget();
        updateSummaries();
        updateEnemyInteractivity();
        networkSession.sendTurn();
    }

    @Override
    public void onWin() {
        if (gameFinished) {
            return;
        }
        networkSession.sendWin();
        finishGame("Victory", "You sank the enemy fleet.");
    }

    @Override
    public void onLose() {
        if (gameFinished) {
            return;
        }
        finishGame("Defeat", "All of your ships were sunk.");
    }

    @Override
    public void onDisconnected(String message) {
        if (gameFinished) {
            return;
        }
        finishGame("Connection Closed", message);
    }

    private GridPane buildPlayerGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = UiFactory.createGridButton();
                button.setDisable(true);
                playerButtons[y][x] = button;
                grid.add(button, x, y);
            }
        }
        return grid;
    }

    private GridPane buildEnemyGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = UiFactory.createGridButton();
                final int finalX = x;
                final int finalY = y;
                button.setOnAction(event -> fireAtEnemy(finalX, finalY));
                enemyButtons[y][x] = button;
                grid.add(button, x, y);
            }
        }
        return grid;
    }

    private void fireAtEnemy(int x, int y) {
        if (gameFinished) {
            return;
        }
        if (!myTurn) {
            statusLabel.setText("Wait for your turn.");
            return;
        }
        if (enemyAttacked[y][x]) {
            statusLabel.setText("That tile was already attacked.");
            return;
        }
        selectedTargetX = x;
        selectedTargetY = y;
        targetLabel.setText("Firing at " + toGridRef(x, y) + "...");
        myTurn = false;
        shotAnimationActive = true;
        updateEnemyInteractivity();

        if (networkSession != null) {
            pendingAttackX = x;
            pendingAttackY = y;
            statusLabel.setText("Firing at " + toGridRef(x, y) + "...");
            animateShot(enemyButtons[y][x], () -> {
                shotAnimationActive = false;
                statusLabel.setText("Shot fired. Waiting for result...");
                networkSession.sendAttack(x, y);
                updateEnemyInteractivity();
            });
            return;
        }
        if (isLocalMultiplayer) {
            shipsVisible = false;
            refreshPlayerGrid();
        }
        statusLabel.setText("Firing at " + toGridRef(x, y) + "...");
        animateShot(enemyButtons[y][x], () -> {
            shotAnimationActive = false;
            AttackOutcome outcome = enemyBoard.receiveAttack(x, y);
            enemyAttacked[y][x] = true;
            enemyHits[y][x] = outcome.getResult() == AttackResult.HIT;

            boolean usedBallistic = ballisticMissileActive;
            if (ballisticMissileActive) {
                ballisticMissileActive = false;
                ballisticButton.setVisible(false);
                ballisticLabel.setVisible(false);
                if (outcome.getResult() == AttackResult.HIT) {
                    outcome = sinkShipWithBallistic(x, y, outcome);
                }
            }

            playerShots++;
            if (outcome.getResult() == AttackResult.HIT) {
                playerHits++;
            }
            log(isLocalMultiplayer ? "Player " + currentPlayer : "You", x, y, outcome);

            markEnemyClearedWater(outcome);
            clearSelectedTarget();
            refreshEnemyGrid();
            if (outcome.getResult() == AttackResult.HIT) {
                statusLabel.setText(outcome.isSunkShip()
                    ? (usedBallistic ? "BALLISTIC STRIKE! You sunk the enemy " + outcome.getShipType().name().toLowerCase() + "!" : "You sunk the enemy " + outcome.getShipType().name().toLowerCase() + "!")
                    : "Hit at " + toGridRef(x, y) + "!");
                audioManager.playExplosion();
            } else {
                statusLabel.setText(usedBallistic ? "Ballistic missile missed. Perk lost." : "Miss at " + toGridRef(x, y) + ".");
            }

            int trackPlayer = isLocalMultiplayer ? currentPlayer : 1;
            updateBallisticStreak(trackPlayer, outcome.isSunkShip(), outcome.getResult() != AttackResult.HIT);

            if (outcome.isGameOver()) {
                if (isLocalMultiplayer) {
                    finishGame("Victory", "Player " + currentPlayer + " wins! All enemy ships were sunk.");
                } else {
                    finishGame("Victory", "You destroyed the AI fleet.");
                }
                return;
            }

            updateSummaries();
            updateEnemyInteractivity();

            if (isLocalMultiplayer) {
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(event -> showHandoffOverlay());
                pause.play();
                return;
            }

            PauseTransition pause = new PauseTransition(Duration.seconds(0.7));
            pause.setOnFinished(event -> executeAiTurn());
            pause.play();
        });
    }

    private void executeAiTurn() {
        if (gameFinished) {
            return;
        }
        Coordinate shot = ai.nextShot();
        shotAnimationActive = true;
        clearSelectedTarget();
        statusLabel.setText("AI is firing at " + toGridRef(shot.x(), shot.y()) + "...");
        animateShot(playerButtons[shot.y()][shot.x()], () -> {
            shotAnimationActive = false;
            AttackOutcome outcome = playerBoard.receiveAttack(shot.x(), shot.y());
            boolean hit = outcome.getResult() == AttackResult.HIT;
            ai.handleShotResult(shot, outcome);
            log("The AI", shot.x(), shot.y(), outcome);
            refreshPlayerGrid();
            updateSummaries();
            if (hit) {
                audioManager.playExplosion();
            }
            if (outcome.isGameOver()) {
                finishGame("Defeat", "The AI destroyed your fleet.");
                return;
            }
            myTurn = true;
            statusLabel.setText(outcome.isSunkShip()
                ? "AI sunk your " + outcome.getShipType().name().toLowerCase() + ". Your turn."
                : "AI fired at " + toGridRef(shot.x(), shot.y()) + ". Your turn.");
            updateEnemyInteractivity();
        });
    }

    private void refreshPlayerGrid() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = playerButtons[y][x];
                if (playerBoard.getTile(x, y).isAttacked()) {
                    if (playerBoard.getTile(x, y).hasShip()) {
                        UiFactory.styleGridButton(button, UiFactory.TileStyle.HIT, "X");
                    } else {
                        UiFactory.styleGridButton(button, UiFactory.TileStyle.MISS, "o");
                    }
                } else if (playerBoard.getTile(x, y).hasShip() && shipsVisible) {
                    UiFactory.styleGridButton(button, UiFactory.TileStyle.SHIP, "S");
                } else {
                    UiFactory.styleGridButton(button, UiFactory.TileStyle.WATER, "");
                }
            }
        }
    }

    private void refreshEnemyGrid() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = enemyButtons[y][x];
                if (!enemyAttacked[y][x]) {
                    UiFactory.styleGridButton(button, UiFactory.TileStyle.WATER, "");
                    if (x == selectedTargetX && y == selectedTargetY) {
                        UiFactory.styleSelectedTargetButton(button);
                    }
                } else if (enemyHits[y][x]) {
                    UiFactory.styleGridButton(button, UiFactory.TileStyle.HIT, "X");
                } else {
                    UiFactory.styleGridButton(button, UiFactory.TileStyle.MISS, "o");
                }
            }
        }
    }

    private void showHandoffOverlay() {
        int nextPlayer = currentPlayer == 1 ? 2 : 1;

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("turn-overlay");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(overlay, Pos.CENTER);

        Label heading = new Label("Player " + nextPlayer + "'s Turn");
        heading.setTextFill(Color.web("#f4fbff"));
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 44));

        Label sub = new Label("Pass the computer to Player " + nextPlayer + ", then press Continue.");
        sub.setTextFill(Color.web("#d7e9ff"));
        sub.setFont(Font.font("Georgia", 20));
        sub.setWrapText(true);

        Button continueBtn = new Button("Continue");
        continueBtn.setPrefWidth(220);
        continueBtn.setPrefHeight(52);
        continueBtn.getStyleClass().add("primary-button");
        continueBtn.setOnAction(e -> {
            audioManager.playSelect();
            root.getChildren().remove(overlay);
            swapLocalPlayer();
        });

        VBox box = new VBox(24, heading, sub, continueBtn);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(600);
        overlay.getChildren().add(box);
        root.getChildren().add(overlay);
    }

    private void swapLocalPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        playerBoard = (currentPlayer == 1) ? player1Board : player2Board;
        enemyBoard = (currentPlayer == 1) ? player2Board : player1Board;
        enemyAttacked = (currentPlayer == 1) ? p1EnemyAttacked : p2EnemyAttacked;
        enemyHits = (currentPlayer == 1) ? p1EnemyHits : p2EnemyHits;
        myTurn = true;
        selectedTargetX = -1;
        selectedTargetY = -1;
        pendingAttackX = -1;
        pendingAttackY = -1;
        shipsVisible = false;
        ballisticLabel.setVisible(false);
        ballisticButton.setVisible(false);
        refreshPlayerGrid();
        refreshEnemyGrid();
        updateSummaries();
        updateEnemyInteractivity();
        statusLabel.setText("Player " + currentPlayer + "'s turn. Ships revealed in 3...");
        PauseTransition reveal = new PauseTransition(Duration.seconds(3));
        reveal.setOnFinished(e -> {
            shipsVisible = true;
            refreshPlayerGrid();
            boolean earned = ballisticEarned[currentPlayer];
            ballisticLabel.setVisible(earned);
            ballisticButton.setVisible(earned);
            ballisticButton.setDisable(ballisticMissileActive);
            ballisticButton.setText(ballisticMissileActive ? "BALLISTIC MISSILE ARMED" : "USE BALLISTIC MISSILE");
            statusLabel.setText("Player " + currentPlayer + "'s turn.");
        });
        reveal.play();
    }

    private void finishGame(String title, String message) {
        gameFinished = true;
        if (difficulty != null && !isLocalMultiplayer && networkSession == null) {
            boolean won = "Victory".equals(title);
            Statistics.get().recordGame(difficulty, won, playerShots, playerHits);
            Achievements.get().evaluate(new Achievements.GameResult(
                difficulty, won, playerShots, playerHits, countSunkPlayerShips()));
        }
        clearSelectedTarget();
        updateEnemyInteractivity();
        if (networkSession != null) {
            networkSession.close();
        }
        finishHandler.finish(title, message);
    }

    /**
     * The log as an overlay rather than a panel beside the boards, because the board layout
     * already fills the smallest window the game allows and has nothing left to give.
     */
    private void showBattleLog() {
        VBox entries = new VBox(4);
        if (battleLog.isEmpty()) {
            Label empty = new Label("Nothing has happened yet.");
            empty.getStyleClass().add("muted-text");
            entries.getChildren().add(empty);
        } else {
            for (String entry : battleLog) {
                Label label = new Label(entry);
                label.getStyleClass().add("body-text");
                entries.getChildren().add(label);
            }
        }

        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(entries);
        scroller.setFitToWidth(true);
        scroller.setPrefViewportHeight(360);
        scroller.getStyleClass().add("terms-scroll");

        VBox panel = new VBox(14, UiFactory.createSectionTitle("Battle Log"), scroller);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        panel.setMaxSize(560, 500);

        StackPane overlay = new StackPane(panel);
        overlay.getStyleClass().add("turn-overlay");
        overlay.setOnMouseClicked(event -> root.getChildren().remove(overlay));

        Button close = UiFactory.createMenuButton("Close", audioManager, () -> root.getChildren().remove(overlay));
        panel.getChildren().add(close);

        root.getChildren().add(overlay);
        // Open at the newest entry, which is the one you just caused.
        javafx.application.Platform.runLater(() -> scroller.setVvalue(1.0));
    }

    private void log(String who, int x, int y, AttackOutcome outcome) {
        String what;
        if (outcome.getResult() != AttackResult.HIT) {
            what = "missed";
        } else if (outcome.isSunkShip() && outcome.getShipType() != null) {
            what = "hit and sank the " + outcome.getShipType().getDisplayName().toLowerCase();
        } else {
            what = "hit";
        }
        battleLog.add(battleLog.size() + 1 + ".  " + who + " " + what + " at " + toGridRef(x, y));
    }

    private int countSunkPlayerShips() {
        int sunk = 0;
        for (game.Ship ship : playerBoard.getShips()) {
            if (ship.isSunk()) {
                sunk++;
            }
        }
        return sunk;
    }

    private String toGridRef(int x, int y) {
        return String.valueOf((char) ('A' + x)) + (y + 1);
    }

    private Label createSummaryLabel() {
        Label label = new Label();
        label.setTextFill(Color.web("#d7e9ff"));
        label.setFont(Font.font("Georgia", 15));
        return label;
    }

    private void updateSummaries() {
        int playerShipsRemaining = (int) playerBoard.getShips().stream().filter(ship -> !ship.isSunk()).count();
        playerSummaryLabel.setText("Ships remaining: " + playerShipsRemaining + "/5");

        if (enemyBoard != null) {
            int enemyShipsRemaining = (int) enemyBoard.getShips().stream().filter(ship -> !ship.isSunk()).count();
            enemySummaryLabel.setText("Enemy ships remaining: " + enemyShipsRemaining + "/5");
        } else {
            int shotsTaken = 0;
            for (int y = 0; y < Board.SIZE; y++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    if (enemyAttacked[y][x]) {
                        shotsTaken++;
                    }
                }
            }
            enemySummaryLabel.setText("Shots fired: " + shotsTaken);
        }
    }

    private void updateEnemyInteractivity() {
        boolean canFire = !gameFinished && !shotAnimationActive && myTurn && pendingAttackX < 0 && pendingAttackY < 0;
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                enemyButtons[y][x].setDisable(!canFire || enemyAttacked[y][x]);
            }
        }
        if (fireButton != null) {
            fireButton.setDisable(true);
        }
        if (passTurnButton != null) {
            passTurnButton.setDisable(!canFire);
        }
        if (targetLabel != null && selectedTargetX < 0 && !shotAnimationActive) {
            targetLabel.setText(canFire ? "Click an enemy square to fire." : "Waiting for turn...");
        }
    }

    private void passTurn() {
        if (gameFinished || !myTurn || shotAnimationActive || pendingAttackX >= 0) {
            return;
        }
        myTurn = false;
        updateEnemyInteractivity();

        if (networkSession != null) {
            statusLabel.setText("Turn passed.");
            networkSession.sendTurn();
            return;
        }
        if (isLocalMultiplayer) {
            statusLabel.setText("Player " + currentPlayer + " passed their turn.");
            PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
            pause.setOnFinished(event -> showHandoffOverlay());
            pause.play();
            return;
        }
        statusLabel.setText("You passed your turn. AI is thinking...");
        PauseTransition pause = new PauseTransition(Duration.seconds(0.7));
        pause.setOnFinished(event -> executeAiTurn());
        pause.play();
    }

    private void animateShot(Button targetButton, Runnable onImpact) {
        audioManager.playFire();
        if (bombImage == null || effectsLayer == null || root == null || root.getScene() == null) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> {
                audioManager.stopFire();
                onImpact.run();
            });
            pause.play();
            return;
        }

        Bounds targetBounds = targetButton.localToScene(targetButton.getBoundsInLocal());
        Point2D targetPoint = effectsLayer.sceneToLocal(
            targetBounds.getMinX() + targetBounds.getWidth() / 2.0,
            targetBounds.getMinY() + targetBounds.getHeight() / 2.0
        );

        ImageView bombView = new ImageView(bombImage);
        bombView.setFitWidth(28);
        bombView.setFitHeight(28);
        bombView.setPreserveRatio(true);
        bombView.setTranslateX(targetPoint.getX() - 14);
        bombView.setTranslateY(targetPoint.getY() - 140);
        effectsLayer.getChildren().add(bombView);

        TranslateTransition drop = new TranslateTransition(Duration.seconds(0.95), bombView);
        drop.setToX(targetPoint.getX() - 14);
        drop.setToY(targetPoint.getY() - 14);
        drop.setOnFinished(event -> effectsLayer.getChildren().remove(bombView));
        drop.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> {
            audioManager.stopFire();
            onImpact.run();
        });
        pause.play();
    }

    private Image loadBombImage() {
        try {
            URL resource = getClass().getResource("/bomb/bomb.png");
            if (resource != null) {
                return new Image(resource.toExternalForm());
            }
        } catch (RuntimeException ignored) {
        }

        File fallback = new File("resources/bomb/bomb.png");
        if (fallback.exists()) {
            try {
                return new Image(fallback.toURI().toString());
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private void clearSelectedTarget() {
        selectedTargetX = -1;
        selectedTargetY = -1;
        if (targetLabel != null) {
            targetLabel.setText("Click an enemy square to fire.");
        }
        if (enemyButtons[0][0] != null) {
            refreshEnemyGrid();
        }
    }

    private void markEnemyClearedWater(AttackOutcome outcome) {
        for (Coordinate coordinate : outcome.getClearedCoordinates()) {
            enemyAttacked[coordinate.y()][coordinate.x()] = true;
            enemyHits[coordinate.y()][coordinate.x()] = false;
        }
    }

    private AttackOutcome sinkShipWithBallistic(int hitX, int hitY, AttackOutcome initialOutcome) {
        Ship ship = enemyBoard.getTile(hitX, hitY).getShip();
        AttackOutcome last = initialOutcome;
        for (Coordinate coord : ship.getCoordinates()) {
            if (coord.x() == hitX && coord.y() == hitY) {
                continue;
            }
            AttackOutcome next = enemyBoard.receiveAttack(coord.x(), coord.y());
            enemyAttacked[coord.y()][coord.x()] = true;
            enemyHits[coord.y()][coord.x()] = true;
            last = next;
        }
        return last;
    }

    private void updateBallisticStreak(int playerIndex, boolean wasSink, boolean wasMiss) {
        if (ballisticEarned[playerIndex]) {
            return;
        }
        if (wasMiss) {
            consecutiveSinks[playerIndex] = 0;
        } else if (wasSink) {
            consecutiveSinks[playerIndex]++;
            if (consecutiveSinks[playerIndex] >= 2) {
                ballisticEarned[playerIndex] = true;
                ballisticLabel.setVisible(true);
                ballisticButton.setVisible(true);
                audioManager.playBallistic();
            }
        }
    }
}
