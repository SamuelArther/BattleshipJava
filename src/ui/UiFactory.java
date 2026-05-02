package ui;

import audio.AudioManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;

/**
 * Builds the pieces of the interface that more than one screen needs.
 *
 * Appearance is left to theme.css: everything here does is attach the style class that
 * says what a node is, so the look can be changed in one file instead of in every scene.
 */
public final class UiFactory {
    private static final String STYLESHEET = "/theme.css";
    private static final String TARGET_CLASS = "tile-target";
    private static final String[] COLUMN_LABELS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    private static final Image BOARD_TEXTURE = loadImage("/board/board.png", "resources/board/board.png");

    /** What a square on the board is currently showing. */
    public enum TileStyle {
        WATER("tile-water"),
        SHIP("tile-ship"),
        HIT("tile-hit"),
        MISS("tile-miss");

        private final String styleClass;

        TileStyle(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    /** One row of a board legend: a coloured swatch and what it means. */
    public record LegendEntry(TileStyle style, String description) {
    }

    private UiFactory() {
    }

    public static LegendEntry entry(TileStyle style, String description) {
        return new LegendEntry(style, description);
    }

    /** Attaches the theme to a scene. Called once per scene, from Main. */
    public static void applyTheme(javafx.scene.Scene scene) {
        URL stylesheet = UiFactory.class.getResource(STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
            return;
        }
        // Running straight from the source tree, where resources/ is not on the classpath.
        File fallback = new File("resources/theme.css");
        if (fallback.exists()) {
            scene.getStylesheets().add(fallback.toURI().toString());
        }
    }

    public static Pane createRootPane() {
        StackPane root = new StackPane();
        root.setPadding(new Insets(18));
        root.getStyleClass().add("root-pane");
        return root;
    }

    public static Label createScreenTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("screen-title");
        return label;
    }

    public static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    public static Button createMenuButton(String text, AudioManager audioManager, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setPrefHeight(50);
        button.getStyleClass().add("menu-button");
        button.setOnAction(event -> {
            audioManager.playSelect();
            action.run();
        });
        return button;
    }

    public static Button createGridButton() {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.setPrefSize(28, 28);
        button.setMinSize(28, 28);
        button.setMaxSize(28, 28);
        button.getStyleClass().add("grid-tile");
        styleGridButton(button, TileStyle.WATER, "");
        return button;
    }

    public static void styleGridButton(Button button, TileStyle style, String text) {
        button.setText(text);
        for (TileStyle candidate : TileStyle.values()) {
            button.getStyleClass().remove(candidate.styleClass);
        }
        button.getStyleClass().remove(TARGET_CLASS);
        button.getStyleClass().add(style.styleClass);
    }

    /** Marks the square the player has picked out but not yet fired at. */
    public static void styleSelectedTargetButton(Button button) {
        if (!button.getStyleClass().contains(TARGET_CLASS)) {
            button.getStyleClass().add(TARGET_CLASS);
        }
    }

    public static BorderPane wrapWithCoordinates(GridPane boardGrid) {
        HBox topLabels = new HBox(3);
        topLabels.setAlignment(Pos.CENTER);
        topLabels.setPadding(new Insets(0, 0, 6, 0));
        for (String labelText : COLUMN_LABELS) {
            Label header = createAxisLabel(labelText);
            header.setMinSize(28, 18);
            header.setPrefSize(28, 18);
            topLabels.getChildren().add(header);
        }

        VBox sideLabels = new VBox(3);
        sideLabels.setAlignment(Pos.CENTER);
        sideLabels.setPadding(new Insets(0, 6, 0, 0));
        for (int i = 1; i <= COLUMN_LABELS.length; i++) {
            Label rowLabel = createAxisLabel(Integer.toString(i));
            rowLabel.setMinSize(18, 28);
            rowLabel.setPrefSize(18, 28);
            sideLabels.getChildren().add(rowLabel);
        }

        BorderPane wrapper = new BorderPane();
        wrapper.setTop(topLabels);
        wrapper.setLeft(sideLabels);
        wrapper.setCenter(boardGrid);
        BorderPane.setAlignment(topLabels, Pos.CENTER);
        BorderPane.setAlignment(sideLabels, Pos.CENTER);
        BorderPane.setAlignment(boardGrid, Pos.CENTER);
        return wrapper;
    }

    public static VBox createLegend(LegendEntry... entries) {
        VBox box = new VBox(5);
        for (LegendEntry entry : entries) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label swatch = new Label("  ");
            swatch.getStyleClass().addAll("legend-swatch", entry.style().styleClass);

            Label text = new Label(entry.description());
            text.getStyleClass().add("muted-text");

            row.getChildren().addAll(swatch, text);
            box.getChildren().add(row);
        }
        return box;
    }

    public static VBox createBoardSection(String title, GridPane boardGrid, VBox legend) {
        Label sectionTitle = createSectionTitle(title);

        BorderPane wrappedBoard = wrapWithCoordinates(boardGrid);
        wrappedBoard.setPadding(new Insets(10));

        StackPane boardFrame = createBoardFrame(wrappedBoard);

        VBox section = new VBox(10, sectionTitle, boardFrame, legend);
        section.setAlignment(Pos.CENTER);
        VBox.setVgrow(boardGrid, Priority.NEVER);
        return section;
    }

    private static StackPane createBoardFrame(Pane wrappedBoard) {
        StackPane frame = new StackPane();
        frame.setPadding(new Insets(10));
        frame.getStyleClass().add("board-frame");

        if (BOARD_TEXTURE != null) {
            ImageView backgroundView = new ImageView(BOARD_TEXTURE);
            backgroundView.setPreserveRatio(true);
            backgroundView.setFitWidth(360);
            backgroundView.setFitHeight(360);
            backgroundView.setOpacity(0.55);
            frame.getChildren().add(backgroundView);
        }

        frame.getChildren().add(wrappedBoard);
        return frame;
    }

    private static Image loadImage(String resourcePath, String fallbackPath) {
        try {
            URL resource = UiFactory.class.getResource(resourcePath);
            if (resource != null) {
                return new Image(resource.toExternalForm());
            }
        } catch (RuntimeException ignored) {
        }
        try {
            File file = new File(fallbackPath);
            if (file.exists()) {
                return new Image(file.toURI().toString());
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static Label createAxisLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("axis-label");
        label.setAlignment(Pos.CENTER);
        label.setMinSize(18, 18);
        label.setPrefSize(18, 18);
        return label;
    }
}
