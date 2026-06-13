package ui;

import audio.AudioManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import settings.Settings;

/**
 * The terms of service, shown once before the first game.
 *
 * Agreement is remembered by version number, so if the terms are ever changed the player is
 * asked again rather than being silently held to something they never read. Declining closes
 * the game, which is the only honest thing a "you must agree" screen can do.
 *
 * I Agree stays disabled until the terms have actually been scrolled to the bottom. Every
 * other piece of software pretends you read them. This one at least makes you scroll past.
 */
public class TermsScene {

    /** Bump this if the terms below ever change, and everyone is asked again. */
    public static final int TERMS_VERSION = 1;

    private static final String[] CLAUSES = {
        "1. THE PEEKING CLAUSE. You shall not look at the other player's grid. Not over their "
            + "shoulder, not in a window reflection, not in the little chrome strip along the top "
            + "of their laptop screen, and not by asking them a question and watching their eyes "
            + "move. Violation terminates this licence immediately and, more importantly, "
            + "terminates the friendship.",

        "2. THE SHIPS DO NOT TOUCH CLAUSE. Ships may not touch one another, including at the "
            + "corners. This is not the game being difficult. This is the navy having standards.",

        "3. THE ANNOUNCEMENT CLAUSE. Upon sinking a vessel you are permitted, but not required, "
            + "to say \"you sank my battleship\" out loud. If the vessel sunk was not in fact the "
            + "Battleship, saying it anyway is a misdemeanour under this agreement and a felony "
            + "under maritime tradition.",

        "4. THE US ARMED FORCES CLAUSE. If you select the difficulty named All of the US Armed "
            + "Forces and then complain that it is unfair, you waive all right to be taken "
            + "seriously for the remainder of the session. It is not cheating. It cannot see your "
            + "board. It is simply better at this than you are, and it would like you to sit with "
            + "that.",

        "5. THE BLAME CLAUSE. Losses are to be attributed to variance, lighting conditions, the "
            + "chair, or the general decline of society. Losses are not to be attributed to the "
            + "opponent being better. This clause is legally unenforceable and morally essential.",

        "6. THE RANDOMIZE CLAUSE. Pressing Randomize until you get a layout you like is "
            + "permitted. Pressing Randomize more than forty times constitutes strategy, and the "
            + "licensor commends you.",

        "7. THE HOT SEAT CLAUSE. During local multiplayer, when the hand-off screen appears, you "
            + "will look away. You will genuinely look away. Turning the monitor slightly and "
            + "claiming you \"didn't really see it\" is a breach of clause 1 wearing a hat.",

        "8. THE JOIN CODE CLAUSE. Join codes are six characters and deliberately contain no "
            + "letter O and no digit 1, so that nobody has to say \"no, the other one\" over a "
            + "kitchen table ever again. Reading your code aloud incorrectly anyway is your own "
            + "affair.",

        "9. THE PARTY CLAUSE. This software contains a disco. Discovery of the disco is left as "
            + "an exercise for the licensee. Starting the disco during a tense endgame is "
            + "permitted, tactically unwise, and extremely funny.",

        "10. THE VOLUME CLAUSE. Sound effects are set to a sensible level by default. If you "
            + "raise the master volume to one hundred percent, put on headphones, and then fire a "
            + "ballistic missile, the licensor accepts no responsibility for your ears, your "
            + "posture, or the noise complaint.",

        "11. THE FIVE SHIPS CLAUSE. You get five ships. Everybody gets five ships. There is no "
            + "sixth ship. Asking about the sixth ship voids nothing but does mark you out.",

        "12. THE ALT-F4 CLAUSE. Quitting while losing is permitted. Quitting while losing and "
            + "then claiming the program crashed is a separate matter between you and your "
            + "conscience.",

        "13. THE WARRANTY CLAUSE. This software is provided as is, with no warranty of any kind, "
            + "express or implied, including but not limited to fitness for a particular purpose, "
            + "merchantability, or the proposition that you will ever beat US Navy difficulty.",

        "14. THE LICENCE CLAUSE. This game is released into the public domain under CC0. You may "
            + "copy it, change it, sell it, or print it out and eat it. Clauses 1 through 13 are "
            + "jokes and carry no legal weight whatsoever. Clause 2 is real, though. The ships "
            + "genuinely do not touch."
    };

    private final AudioManager audioManager;
    private final Runnable acceptAction;
    private final Runnable declineAction;

    public TermsScene(AudioManager audioManager, Runnable acceptAction, Runnable declineAction) {
        this.audioManager = audioManager;
        this.acceptAction = acceptAction;
        this.declineAction = declineAction;
    }

    public Scene createScene() {
        Pane root = UiFactory.createRootPane();

        Label title = UiFactory.createScreenTitle("Terms of Service");

        Label preamble = new Label("Please read the following in full. You will be asked to agree "
            + "to it, and unlike every other agreement you have ever accepted, this one is short "
            + "enough that you actually could.");
        preamble.getStyleClass().add("body-text");
        preamble.setWrapText(true);
        preamble.setMaxWidth(760);

        VBox clauses = new VBox(14);
        clauses.setPadding(new Insets(4, 18, 4, 4));
        for (String clause : CLAUSES) {
            Label label = new Label(clause);
            label.getStyleClass().add("muted-text");
            label.setWrapText(true);
            label.setMaxWidth(720);
            clauses.getChildren().add(label);
        }

        ScrollPane scroller = new ScrollPane(clauses);
        scroller.setFitToWidth(true);
        scroller.setPrefViewportHeight(330);
        scroller.setMaxWidth(770);
        scroller.getStyleClass().add("terms-scroll");

        Button agree = UiFactory.createMenuButton("I Agree", audioManager, acceptAction);
        agree.getStyleClass().add("primary-button");
        agree.setDisable(true);

        Label hint = new Label("Scroll to the end of the terms to continue.");
        hint.getStyleClass().add("muted-text");

        // Enable only once the terms have genuinely been scrolled through.
        scroller.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.985) {
                agree.setDisable(false);
                hint.setText("Thank you for reading the terms. Nobody ever does.");
                settings.Achievements.get().unlock(settings.Achievements.Achievement.LEGAL_SCHOLAR);
            }
        });
        // A viewport tall enough to show everything at once has nothing to scroll.
        clauses.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() <= scroller.getViewportBounds().getHeight()) {
                agree.setDisable(false);
            }
        });

        Button decline = UiFactory.createMenuButton("Decline", audioManager, declineAction);
        decline.getStyleClass().add("danger-button");

        HBox buttons = new HBox(16, agree, decline);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, title, preamble, scroller, hint, buttons);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(800);

        root.getChildren().add(content);
        return new Scene(root, Settings.get().getWindowWidth(), Settings.get().getWindowHeight());
    }
}
