package ui;

import ai.BattleshipAI;
import ai.Difficulty;
import audio.AudioManager;
import game.AttackOutcome;
import game.AttackResult;
import game.Board;
import game.Coordinate;
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
    private final Board playerBoard;
    private final FinishHandler finishHandler;
    private final Runnable menuAction;
    private final Button[][] playerButtons = new Button[Board.SIZE][Board.SIZE];
    private final Button[][] enemyButtons = new Button[Board.SIZE][Board.SIZE];
    private final boolean[][] enemyAttacked = new boolean[Board.SIZE][Board.SIZE];
    private final boolean[][] enemyHits = new boolean[Board.SIZE][Board.SIZE];

    private Label statusLabel;
    private Label playerSummaryLabel;
    private Label enemySummaryLabel;
    private Label targetLabel;
    private Button fireButton;
    private boolean myTurn;
    private boolean gameFinished;
    private boolean shotAnimationActive;
    private int selectedTargetX = -1;
    private int selectedTargetY = -1;
    private int pendingAttackX = -1;
    private int pendingAttackY = -1;

    private Board enemyBoard;
    private BattleshipAI ai;
    private NetworkGameSession networkSession;
    private StackPane root;
    private Pane effectsLayer;
    private Image bombImage;

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

        statusLabel = new Label(myTurn ? "Your turn." : "Opponent's turn.");
        statusLabel.setTextFill(Color.web("#d7e9ff"));
        statusLabel.setFont(Font.font("Georgia", 18));

        VBox topBox = new VBox(8, title, statusLabel);
        topBox.setPadding(new Insets(0, 0, 18, 0));

        playerSummaryLabel = createSummaryLabel();
        enemySummaryLabel = createSummaryLabel();

        VBox playerBox = UiFactory.createBoardSection(
            "Your Grid",
            buildPlayerGrid(),
            UiFactory.createLegend(
                "#90caf9|Your ship",
                "#ff6b6b|Hit",
                "#9aa5b1|Miss"
            )
        );
        playerBox.getChildren().add(playerSummaryLabel);

        VBox enemyBox = UiFactory.createBoardSection(
            "Enemy Grid",
            buildEnemyGrid(),
            UiFactory.createLegend(
                "#b9d7ea|Unfired tile",
                "#ff6b6b|Confirmed hit",
                "#9aa5b1|Confirmed miss"
            )
        );
        enemyBox.getChildren().add(enemySummaryLabel);

        targetLabel = new Label("Click an enemy square to fire.");
        targetLabel.setTextFill(Color.web("#f4fbff"));
        targetLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 16));

        fireButton = new Button("Fire!");
        fireButton.setPrefWidth(180);
        fireButton.setPrefHeight(48);
        fireButton.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #d62828; -fx-text-fill: white; -fx-border-color: #ffffff; -fx-border-width: 2;");
        fireButton.setManaged(false);
        fireButton.setVisible(false);

        VBox attackBox = new VBox(10, targetLabel, fireButton);
        attackBox.setAlignment(Pos.CENTER);
        enemyBox.getChildren().add(attackBox);

        HBox centerBox = new HBox(36, playerBox, enemyBox);
        centerBox.setAlignment(Pos.TOP_CENTER);

        Button menuButton = UiFactory.createMenuButton("Main Menu", audioManager, () -> {
            if (networkSession != null) {
                networkSession.close();
            }
            menuAction.run();
        });
        VBox bottomBox = new VBox(12, menuButton);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(18, 0, 28, 0));

        layout.setTop(topBox);
        layout.setCenter(centerBox);
        layout.setBottom(bottomBox);

        root.getChildren().addAll(layout, effectsLayer);
        refreshPlayerGrid();
        refreshEnemyGrid();
        updateSummaries();
        updateEnemyInteractivity();
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
        if (!playerBoard.isInBounds(x, y)) {
            finishGame("Connection Closed", "An illegal network move was detected.");
            return;
        }
        animateShot(playerButtons[y][x], () -> {
            AttackOutcome outcome = playerBoard.receiveAttack(x, y);
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
        grid.setHgap(3);
        grid.setVgap(3);
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
        grid.setHgap(3);
        grid.setVgap(3);
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
        statusLabel.setText("Firing at " + toGridRef(x, y) + "...");
        animateShot(enemyButtons[y][x], () -> {
            shotAnimationActive = false;
            AttackOutcome outcome = enemyBoard.receiveAttack(x, y);
            enemyAttacked[y][x] = true;
            enemyHits[y][x] = outcome.getResult() == AttackResult.HIT;
            clearSelectedTarget();
            refreshEnemyGrid();
            if (outcome.getResult() == AttackResult.HIT) {
                statusLabel.setText("Hit at " + toGridRef(x, y) + "!");
                audioManager.playExplosion();
            } else {
                statusLabel.setText("Miss at " + toGridRef(x, y) + ".");
            }

            if (outcome.isGameOver()) {
                finishGame("Victory", "You destroyed the AI fleet.");
                return;
            }

            updateSummaries();
            updateEnemyInteractivity();
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
            ai.handleShotResult(shot, hit);
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
            statusLabel.setText("AI fired at " + toGridRef(shot.x(), shot.y()) + ". Your turn.");
            updateEnemyInteractivity();
        });
    }

    private void refreshPlayerGrid() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = playerButtons[y][x];
                if (playerBoard.getTile(x, y).isAttacked()) {
                    if (playerBoard.getTile(x, y).hasShip()) {
                        UiFactory.styleGridButton(button, "#ff6b6b", "X");
                    } else {
                        UiFactory.styleGridButton(button, "#9aa5b1", "o");
                    }
                } else if (playerBoard.getTile(x, y).hasShip()) {
                    UiFactory.styleGridButton(button, "#90caf9", "S");
                } else {
                    UiFactory.styleGridButton(button, "#b9d7ea", "");
                }
            }
        }
    }

    private void refreshEnemyGrid() {
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                Button button = enemyButtons[y][x];
                if (!enemyAttacked[y][x]) {
                    UiFactory.styleGridButton(button, "#b9d7ea", "");
                    if (x == selectedTargetX && y == selectedTargetY) {
                        UiFactory.styleSelectedTargetButton(button);
                    }
                } else if (enemyHits[y][x]) {
                    UiFactory.styleGridButton(button, "#ff6b6b", "X");
                } else {
                    UiFactory.styleGridButton(button, "#9aa5b1", "o");
                }
            }
        }
    }

    private void finishGame(String title, String message) {
        gameFinished = true;
        clearSelectedTarget();
        updateEnemyInteractivity();
        if (networkSession != null) {
            networkSession.close();
        }
        finishHandler.finish(title, message);
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
        if (targetLabel != null && selectedTargetX < 0 && !shotAnimationActive) {
            targetLabel.setText(canFire ? "Click an enemy square to fire." : "Waiting for turn...");
        }
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
}
