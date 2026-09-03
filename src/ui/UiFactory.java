package ui;

import audio.AudioManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.net.URL;

public final class UiFactory {
    private static final String[] COLUMN_LABELS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    private static final Image BOARD_TEXTURE = loadImage("/board/board.png", "resources/board/board.png");

    private UiFactory() {
    }

    public static Pane createRootPane() {
        StackPane root = new StackPane();
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(Color.web("#0b1f33"), CornerRadii.EMPTY, Insets.EMPTY)));
        return root;
    }

    public static Button createMenuButton(String text, AudioManager audioManager, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(240);
        button.setPrefHeight(48);
        button.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #d7e9ff; -fx-text-fill: #0b1f33;");
        button.setOnAction(event -> {
            audioManager.playSelect();
            action.run();
        });
        return button;
    }

    public static Button createGridButton() {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.setPrefSize(30, 30);
        button.setMinSize(30, 30);
        button.setMaxSize(30, 30);
        styleGridButton(button, "#b9d7ea", "");
        return button;
    }

    public static void styleGridButton(Button button, String color, String text) {
        button.setText(text);
        button.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-color: " + color + "; -fx-text-fill: #0b1f33; -fx-border-color: #0b1f33; -fx-border-width: 1;");
    }

    public static BorderPane wrapWithCoordinates(GridPane boardGrid) {
        HBox topLabels = new HBox(3);
        topLabels.setAlignment(Pos.CENTER);
        topLabels.setPadding(new Insets(0, 0, 6, 0));
        for (String labelText : COLUMN_LABELS) {
            Label header = createAxisLabel(labelText);
            header.setMinSize(30, 24);
            header.setPrefSize(30, 24);
            topLabels.getChildren().add(header);
        }

        VBox sideLabels = new VBox(3);
        sideLabels.setAlignment(Pos.CENTER);
        sideLabels.setPadding(new Insets(0, 6, 0, 0));
        for (int i = 1; i <= COLUMN_LABELS.length; i++) {
            Label rowLabel = createAxisLabel(Integer.toString(i));
            rowLabel.setMinSize(24, 30);
            rowLabel.setPrefSize(24, 30);
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

    public static VBox createLegend(String... entries) {
        VBox box = new VBox(8);
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 2);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label swatch = new Label("  ");
            swatch.setMinSize(18, 18);
            swatch.setStyle("-fx-background-color: " + parts[0] + "; -fx-border-color: #0b1f33; -fx-border-width: 1;");

            Label text = new Label(parts.length > 1 ? parts[1] : "");
            text.setTextFill(Color.web("#d7e9ff"));
            row.getChildren().addAll(swatch, text);
            box.getChildren().add(row);
        }
        return box;
    }

    public static VBox createBoardSection(String title, GridPane boardGrid, VBox legend) {
        Label sectionTitle = new Label(title);
        sectionTitle.setTextFill(Color.web("#f4fbff"));
        sectionTitle.setFont(Font.font("Georgia", FontWeight.BOLD, 22));

        BorderPane wrappedBoard = wrapWithCoordinates(boardGrid);
        wrappedBoard.setPadding(new Insets(18));

        StackPane boardFrame = createBoardFrame(wrappedBoard);

        VBox section = new VBox(12, sectionTitle, boardFrame, legend);
        section.setAlignment(Pos.CENTER);
        VBox.setVgrow(boardGrid, Priority.NEVER);
        return section;
    }

    private static StackPane createBoardFrame(Pane wrappedBoard) {
        StackPane frame = new StackPane();
        frame.setPadding(new Insets(10));
        frame.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255, 0.12), new CornerRadii(12), Insets.EMPTY)));
        frame.setStyle("-fx-border-color: #f4fbff; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        if (BOARD_TEXTURE != null) {
            ImageView backgroundView = new ImageView(BOARD_TEXTURE);
            backgroundView.setPreserveRatio(true);
            backgroundView.setFitWidth(390);
            backgroundView.setFitHeight(390);
            backgroundView.setOpacity(0.95);
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
        label.setTextFill(Color.web("#f4fbff"));
        label.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        label.setAlignment(Pos.CENTER);
        label.setMinSize(24, 24);
        label.setPrefSize(24, 24);
        return label;
    }
}
