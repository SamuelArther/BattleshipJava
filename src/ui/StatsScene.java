package ui;

import ai.Difficulty;
import audio.AudioManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import settings.Achievements;
import settings.Settings;
import settings.Statistics;

/**
 * The player's record against the computer.
 *
 * Only single-player games appear here, which is what makes the numbers mean anything: a
 * hot-seat game has no result that belongs to one person, and a network game is decided as
 * much by the opponent as by you.
 */
public class StatsScene {

    private final AudioManager audioManager;
    private final Runnable backAction;
    private final Statistics statistics = Statistics.get();

    public StatsScene(AudioManager audioManager, Runnable backAction) {
        this.audioManager = audioManager;
        this.backAction = backAction;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        VBox content = new VBox(18,
            UiFactory.createScreenTitle("Statistics"),
            buildSummary(),
            buildTable(),
            buildAchievements(),
            buildButtons());
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(820);
        content.setPadding(new Insets(4, 18, 18, 4));

        // There is more here than fits the smallest window, so the whole page scrolls.
        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("page-scroll");

        root.getChildren().add(scroller);
        return new Scene(root, Settings.get().getWindowWidth(), Settings.get().getWindowHeight());
    }

    private VBox buildSummary() {
        int played = statistics.getGamesPlayed();

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(9);
        grid.setAlignment(Pos.CENTER);

        if (played == 0) {
            Label empty = new Label("No games yet. Beat the computer once and this fills in.");
            empty.getStyleClass().add("muted-text");
            VBox panel = new VBox(12, UiFactory.createSectionTitle("Overall"), empty);
            panel.getStyleClass().add("panel");
            panel.setPadding(new Insets(20, 24, 22, 24));
            return panel;
        }

        stat(grid, 0, 0, "Games played", Integer.toString(played));
        stat(grid, 1, 0, "Won", Integer.toString(statistics.getGamesWon()));
        stat(grid, 2, 0, "Lost", Integer.toString(statistics.getGamesLost()));
        stat(grid, 3, 0, "Win rate", percent(statistics.getWinRate()));

        stat(grid, 0, 1, "Shots fired", Long.toString(statistics.getShotsFired()));
        stat(grid, 1, 1, "Accuracy", percent(statistics.getAccuracy()));
        stat(grid, 2, 1, "Current streak", Integer.toString(statistics.getCurrentStreak()));
        stat(grid, 3, 1, "Best streak", Integer.toString(statistics.getLongestStreak()));

        Difficulty highest = statistics.getHighestCleared();
        Label cleared = new Label(highest == null
            ? "You have not beaten any difficulty yet."
            : "Hardest difficulty beaten: " + highest);
        cleared.getStyleClass().add("body-text");

        VBox panel = new VBox(14, UiFactory.createSectionTitle("Overall"), grid, cleared);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        return panel;
    }

    private void stat(GridPane grid, int column, int row, String name, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("muted-text");

        VBox box = new VBox(1, valueLabel, nameLabel);
        box.setAlignment(Pos.CENTER);
        grid.add(box, column, row);
    }

    private VBox buildTable() {
        GridPane table = new GridPane();
        table.setHgap(18);
        table.setVgap(7);
        for (int i = 0; i < 5; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(i == 0 ? Priority.ALWAYS : Priority.NEVER);
            column.setMinWidth(i == 0 ? 210 : 78);
            table.getColumnConstraints().add(column);
        }

        header(table, 0, "Difficulty");
        header(table, 1, "Played");
        header(table, 2, "Won");
        header(table, 3, "Win rate");
        header(table, 4, "Best");

        int row = 1;
        for (Difficulty difficulty : Difficulty.values()) {
            Statistics.Record record = statistics.recordFor(difficulty);
            cell(table, 0, row, difficulty.toString(), record.played() > 0);
            cell(table, 1, row, Integer.toString(record.played()), record.played() > 0);
            cell(table, 2, row, Integer.toString(record.won()), record.played() > 0);
            cell(table, 3, row, record.played() == 0 ? "—" : percent(record.winRate()), record.played() > 0);
            // "Best" is the fewest shots taken to win at this level.
            cell(table, 4, row, record.hasBest() ? Integer.toString(record.bestShots()) : "—", record.hasBest());
            row++;
        }

        Label note = new Label("Best is the fewest shots you have ever needed to clear the board at that level. "
            + "Firing at every square would take a hundred.");
        note.getStyleClass().add("muted-text");
        note.setWrapText(true);

        VBox panel = new VBox(14, UiFactory.createSectionTitle("By difficulty"), table, note);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        return panel;
    }

    private void header(GridPane table, int column, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-header");
        table.add(label, column, 0);
    }

    private void cell(GridPane table, int column, int row, String text, boolean active) {
        Label label = new Label(text);
        label.getStyleClass().add(active ? "body-text" : "muted-text");
        table.add(label, column, row);
    }

    private VBox buildAchievements() {
        Achievements achievements = Achievements.get();
        int total = Achievements.Achievement.values().length;

        VBox list = new VBox(6);
        for (Achievements.Achievement achievement : Achievements.Achievement.values()) {
            boolean unlocked = achievements.isUnlocked(achievement);
            Label label = new Label((unlocked ? "✓  " : "—  ")
                + achievement.getDisplayName() + " — " + achievement.getDescription());
            label.getStyleClass().add(unlocked ? "achievement-done" : "achievement-locked");
            label.setWrapText(true);
            list.getChildren().add(label);
        }

        Label heading = UiFactory.createSectionTitle(
            "Achievements  (" + achievements.unlockedCount() + " of " + total + ")");

        VBox panel = new VBox(14, heading, list);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(20, 24, 22, 24));
        return panel;
    }

    private HBox buildButtons() {
        Button back = UiFactory.createMenuButton("Back", audioManager, backAction);

        Button reset = UiFactory.createMenuButton("Reset statistics", audioManager, () -> { });
        reset.getStyleClass().add("danger-button");
        // Two presses, because there is no undo once the file is rewritten.
        reset.setOnAction(event -> {
            audioManager.playSelect();
            if ("Reset statistics".equals(reset.getText())) {
                reset.setText("Press again to erase");
                return;
            }
            statistics.reset();
            backAction.run();
        });

        HBox row = new HBox(16, back, reset);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private static String percent(double value) {
        return Math.round(value * 100) + "%";
    }
}
