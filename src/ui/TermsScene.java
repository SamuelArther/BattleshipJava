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
    public static final int TERMS_VERSION = 2;

    /** A numbered part of the agreement, and the clauses under it. */
    private record Part(String heading, String... clauses) {
    }

    private static final Part[] PARTS = {
        new Part("PART I — DEFINITIONS",

            "1.1  \"The Licensor\" means whoever wrote this software. At the time of writing this "
                + "is believed to be one person and a computer, and the division of labour between "
                + "them is not a matter this agreement wishes to examine closely.",

            "1.2  \"The Licensee\" means you. Yes, you specifically. Not the household, not the "
                + "person who set the machine up, and not the friend who is about to lean over your "
                + "shoulder in a manner addressed at length in Part II.",

            "1.3  \"Vessel\" means any of the five ships: the Carrier, the Battleship, the Cruiser, "
                + "the Submarine and the Destroyer. There is no sixth vessel. The Licensor is aware "
                + "that you have thought of one. It does not exist.",

            "1.4  \"The Grid\" means the ten-by-ten field of squares on which hostilities take "
                + "place. A square is identified by its letter followed by its number, in that "
                + "order. Identifying a square as \"5E\" is not a breach of this agreement but is "
                + "noted permanently in your character.",

            "1.5  \"Peeking\" means acquiring, by any means, information about the disposition of "
                + "an opponent's fleet that the game has not given you. The definition is "
                + "deliberately broad. It was drafted by somebody who has been peeked at.",

            "1.6  \"A Reasonable Person\" means, for the purposes of this agreement, a person who "
                + "does not peek. The Licensor accepts that this sets a higher bar than most legal "
                + "systems and considers the higher bar to be the point."),

        new Part("PART II — CONDUCT OF THE LICENSEE",

            "2.1  THE PEEKING CLAUSE. You shall not look at the other player's grid. This "
                + "prohibition subsists whether the looking is deliberate, opportunistic, or "
                + "described afterwards as accidental.",

            "2.2  Without limiting the generality of clause 2.1, the following are peeking: looking "
                + "over their shoulder; looking at their screen's reflection in a window, a mirror, "
                + "a television that is switched off, a picture frame, or the polished side of a "
                + "kettle; and reading the little chrome strip along the top of their laptop lid.",

            "2.3  Also peeking: asking your opponent a question and watching their eyes move to the "
                + "square they are worried about. This is the most sophisticated breach available "
                + "to an amateur and the Licensor has a grudging respect for it. It remains a "
                + "breach.",

            "2.4  Breach of any clause in Part II terminates this licence immediately and, more "
                + "importantly, terminates the friendship. The friendship is not restored by winning.",

            "2.5  THE HOT SEAT CLAUSE. During local multiplayer, when the hand-off screen appears, "
                + "you will look away. Not glance away. Look away. The hand-off screen exists "
                + "solely to protect you from your own worst instincts and it cannot do that alone.",

            "2.6  Turning the monitor slightly and later claiming you \"didn't really see it\" is a "
                + "breach of clause 2.1 wearing a hat. The hat is not a defence.",

            "2.7  THE ANNOUNCEMENT CLAUSE. Upon sinking a vessel you are permitted, but not "
                + "required, to say \"you sank my battleship\" out loud.",

            "2.8  If the vessel sunk was not in fact the Battleship, saying it anyway is a "
                + "misdemeanour under this agreement and a felony under maritime tradition. The "
                + "game will tell you which vessel you sank. It goes to some trouble to do this.",

            "2.9  THE GLOATING CLAUSE. Celebration proportionate to the achievement is encouraged. "
                + "Celebration following a win against Easy is not proportionate to anything and "
                + "will be remembered by everyone present for longer than you would like.",

            "2.10  THE REMATCH CLAUSE. \"Best of three\" may be declared at any point before the "
                + "final vessel is sunk. Declaring it afterwards is not a rematch, it is a "
                + "negotiation, and the other party is under no obligation to entertain it."),

        new Part("PART III — PLACEMENT OF THE FLEET",

            "3.1  THE SHIPS DO NOT TOUCH CLAUSE. Vessels may not touch one another. Not along an "
                + "edge, and not at a corner. Every vessel requires at least one square of open "
                + "water around it on all sides, diagonals included.",

            "3.2  This is not the game being difficult. This is the navy having standards. It is "
                + "also, unlike most of this document, load-bearing: the computer opponent relies "
                + "on it, and the moment a vessel sinks, every square touching it is known to be "
                + "empty water and will not be fired upon again.",

            "3.3  THE RANDOMIZE CLAUSE. Pressing Randomize until you are given a layout you like is "
                + "expressly permitted and requires no disclosure to your opponent.",

            "3.4  Pressing Randomize more than forty times in a single session constitutes "
                + "strategy. The Licensor commends you and quietly wonders what you are looking for.",

            "3.5  THE CORNER CLAUSE. Putting every vessel along the edges of the grid is legal, "
                + "traditional, and the first thing every opponent checks. It is not a plan. It is "
                + "a habit that has survived because nobody says anything."),

        new Part("PART IV — DIFFICULTY, AND THE ATTRIBUTION OF DEFEAT",

            "4.1  THE US ARMED FORCES CLAUSE. If you select the difficulty named All of the US "
                + "Armed Forces and subsequently complain that it is unfair, you waive all right to "
                + "be taken seriously for the remainder of the session.",

            "4.2  It is not cheating. It cannot see your board. It is given precisely what you are "
                + "given, which is hit, miss, and sunk, and nothing else whatsoever. What it does "
                + "with that is rebuild a map of every arrangement your surviving fleet could still "
                + "be in, every single turn, and fire at whichever square appears in the most of "
                + "them. It is simply better at this than you are, and it would like you to sit "
                + "with that.",

            "4.3  THE BLAME CLAUSE. Defeats are to be attributed to variance, lighting conditions, "
                + "the chair, the angle of the desk, or the general decline of society.",

            "4.4  Defeats are not to be attributed to the opponent having played better. This "
                + "clause is legally unenforceable and morally essential.",

            "4.5  THE VARIANCE CLAUSE. You are entitled to describe any loss as bad luck and any "
                + "win as sound judgement. Every player does this. The Licensor does this. Nothing "
                + "in this agreement requires consistency of you.",

            "4.6  THE RIGGED CLAUSE. The computer does not move your vessels once placed, does not "
                + "know where they are before it fires, and does not adjust its aim based on "
                + "anything except the results of its own previous shots. Allegations to the "
                + "contrary may be raised, at volume, and will be entertained by nobody."),

        new Part("PART V — TECHNICAL PROVISIONS",

            "5.1  THE VOLUME CLAUSE. Sound is set to a sensible level by default and every part of "
                + "it can be adjusted in Settings, including separately for music and effects.",

            "5.2  If you raise the master volume to one hundred percent, put on headphones, and "
                + "then fire a ballistic missile, the Licensor accepts no responsibility for your "
                + "ears, your posture, the chair, or the noise complaint.",

            "5.3  THE PARTY CLAUSE. This software contains a disco. Discovery of the disco is left "
                + "as an exercise for the Licensee, save to note that it is not on a menu and never "
                + "will be.",

            "5.4  Starting the disco during a tense endgame is permitted, tactically unwise, and "
                + "extremely funny. Starting it during someone else's tense endgame is all three of "
                + "those and also a provocation.",

            "5.5  THE THEME CLAUSE. This software can be made to look like Windows 98. The Licensor "
                + "makes no representation as to whether this is nostalgia or a cry for help and "
                + "declines to investigate the question.",

            "5.6  THE ALT-F4 CLAUSE. Quitting while losing is permitted. Every player has done it. "
                + "It is not a breach of anything.",

            "5.7  Quitting while losing and then claiming the program crashed is a separate matter "
                + "between you and your conscience, in which the Licensor takes no part and holds "
                + "no evidence, the game having no idea why it was closed.",

            "5.8  THE JOIN CODE CLAUSE. Join codes are six characters and deliberately contain no "
                + "letter O and no digit 1, so that nobody has to say \"no, the other one\" across a "
                + "kitchen table ever again. Reading your code aloud incorrectly regardless is your "
                + "own affair and the Licensor has done what it can."),

        new Part("PART VI — WARRANTY, LIABILITY AND RECORD-KEEPING",

            "6.1  THE WARRANTY CLAUSE. This software is provided as is, without warranty of any "
                + "kind, express or implied, including but not limited to warranties of "
                + "merchantability, fitness for a particular purpose, and non-infringement.",

            "6.2  Without limiting clause 6.1, no warranty whatsoever is given that you will ever "
                + "beat US Navy difficulty. Several people have. It is not impossible. It is "
                + "merely not promised.",

            "6.3  THE STATISTICS CLAUSE. The game keeps a record of your games, your accuracy and "
                + "your win rate, and shows them to you on request. The numbers do not lie. This "
                + "clause makes no representation about anybody else in the room.",

            "6.4  Statistics may be erased at any time from the Statistics screen. The Licensor "
                + "notes without comment that this facility exists, that it asks twice, and that "
                + "it is used most often immediately after a losing streak.",

            "6.5  THE ACHIEVEMENTS CLAUSE. Achievements are awarded for things that actually "
                + "happened in a game you actually played. None of them is awarded for turning up. "
                + "One of them is awarded for reading this document to the end, which should tell "
                + "you what the Licensor expects the completion rate to be."),

        new Part("PART VII — GENERAL",

            "7.1  GOVERNING LAW. This agreement is governed by maritime tradition, which is older "
                + "than any statute, entirely unwritten, and enforced exclusively by the withering "
                + "silence of people who know what you did.",

            "7.2  SEVERABILITY. If any clause of this agreement is found to be unenforceable, that "
                + "clause shall be severed and the remainder shall continue in full force. Given "
                + "the contents of Parts I through VI, this may take a while and will not leave "
                + "much behind.",

            "7.3  ENTIRE AGREEMENT. This document constitutes the entire agreement between the "
                + "parties and supersedes all prior representations, including anything the "
                + "Licensor may have said about being bad at this game, which was said to lower "
                + "your guard and is hereby withdrawn.",

            "7.4  AMENDMENT. These terms may be amended. If they are, you will be asked to agree "
                + "again, because being quietly held to something you never saw is exactly the "
                + "trick this document is making fun of.",

            "7.5  THE LICENCE CLAUSE. This game is released into the public domain under CC0 1.0 "
                + "Universal. You may copy it, change it, sell it, put your own name on it, or "
                + "print it out and eat it. No permission is required and none is being granted, "
                + "because none was ever needed.",

            "7.6  Clauses 1.1 through 7.4 are jokes and carry no legal weight of any kind. Clause "
                + "7.5 is real. So is clause 3.1: the ships genuinely do not touch. Thank you for "
                + "reading the terms. Nobody ever does.")
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

        Label preamble = new Label("Please read the following in full. It runs to seven parts and "
            + "forty-six clauses, which is longer than it was and considerably shorter than the "
            + "one you agreed to this morning without reading a word of.");
        preamble.getStyleClass().add("body-text");
        preamble.setWrapText(true);
        preamble.setMaxWidth(760);

        VBox body = new VBox(10);
        body.setPadding(new Insets(4, 18, 4, 4));
        for (Part part : PARTS) {
            Label heading = new Label(part.heading());
            heading.getStyleClass().add("terms-part");
            VBox.setMargin(heading, new Insets(part == PARTS[0] ? 0 : 14, 0, 2, 0));
            body.getChildren().add(heading);

            for (String clause : part.clauses()) {
                Label label = new Label(clause);
                label.getStyleClass().add("muted-text");
                label.setWrapText(true);
                label.setMaxWidth(720);
                body.getChildren().add(label);
            }
        }

        ScrollPane scroller = new ScrollPane(body);
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
        body.heightProperty().addListener((observable, oldValue, newValue) -> {
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
