package settings;

import ai.Difficulty;
import game.ShipType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Things worth doing, and whether you have done them. A hundred and twenty-five of them.
 *
 * Everything here is checked against what actually happened rather than being handed out for
 * turning up. Nothing unlocks twice, and nothing unlocks retroactively from statistics that
 * were already banked, because an achievement you never saw is not much of a reward.
 *
 * The list is grouped into categories so that the statistics screen can show it as something
 * other than a wall of a hundred and twenty-five lines. Three kinds of thing are recorded:
 * the achievements themselves, a handful of running counts (how many boards you have cleared,
 * how many hot-seat games you have finished), and one-off flags for things that have happened
 * at least once (you have hosted a game, you have seen the Abyss theme). All three live in the
 * same file beside the settings.
 *
 * What counts, by mode:
 *
 *   Single player   everything. The lifetime tallies, the difficulty ladder and the streaks all
 *                   read from Statistics, which counts single-player games only, for the same
 *                   reason Statistics gives.
 *   Network         the things one player can be judged on alone: how you shot, what it cost
 *                   you, how your fleet was laid out. A peer is told only hit or miss, so
 *                   nothing about sinking is claimed.
 *   Local hot seat  only the hot-seat achievements. Two people share the board, so no single
 *                   player's gunnery can be read off it honestly.
 */
public final class Achievements {

    /** A group of achievements, shown as one section on the statistics screen. */
    public enum Category {
        CAMPAIGN("Campaign"),
        DIFFICULTY("Difficulty"),
        PRECISION("Precision"),
        STREAKS("Streaks"),
        FLEET("Your fleet"),
        GUNNERY("Gunnery"),
        GRID("The grid"),
        ORDNANCE("Ordnance"),
        PLACEMENT("Placement"),
        MODES("Ways to play"),
        PRESENTATION("Presentation"),
        CURIOSITIES("Curiosities");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Achievement {
        FIRST_BLOOD("First Blood", "Win a game.", Category.CAMPAIGN),
        SHAKEDOWN("Shakedown Cruise", "Win five games.", Category.CAMPAIGN),
        TEN_HULLS("Ten Hulls", "Win ten games.", Category.CAMPAIGN),
        QUARTERDECK("Quarterdeck", "Win twenty-five games.", Category.CAMPAIGN),
        FIFTY_FATHOMS("Fifty Fathoms", "Win fifty games.", Category.CAMPAIGN),
        CENTURION("Centurion", "Win a hundred games.", Category.CAMPAIGN),
        FLEET_ADMIRAL("Fleet Admiral", "Win two hundred and fifty games.", Category.CAMPAIGN),
        BAPTISM("Baptism", "Lose your first game.", Category.CAMPAIGN),
        THICK_SKIN("Thick Skin", "Lose fifty games.", Category.CAMPAIGN),
        FEET_WET("Feet Wet", "Play ten games.", Category.CAMPAIGN),
        VETERAN("Veteran", "Play fifty games.", Category.CAMPAIGN),
        HUNDRED_SORTIES("Hundred Sorties", "Play a hundred games.", Category.CAMPAIGN),
        OLD_SALT("Old Salt", "Play five hundred games.", Category.CAMPAIGN),
        THOUSAND_YARD("Thousand-Yard Stare", "Play a thousand games.", Category.CAMPAIGN),
        SEA_TRIALS("Sea Trials", "Beat Easy.", Category.DIFFICULTY),
        BLUE_WATER("Blue Water", "Beat Medium.", Category.DIFFICULTY),
        HARD_APORT("Hard Aport", "Beat Hard.", Category.DIFFICULTY),
        NIGHT_WATCH("Night Watch", "Beat Nightmare.", Category.DIFFICULTY),
        ADMIRAL("Admiral", "Beat US Navy.", Category.DIFFICULTY),
        JOINT_CHIEFS("Joint Chiefs", "Beat All of the US Armed Forces.", Category.DIFFICULTY),
        ALL_HANDS("All Hands", "Beat every difficulty at least once.", Category.DIFFICULTY),
        MILK_RUN("Milk Run", "Beat Easy ten times.", Category.DIFFICULTY),
        JOURNEYMAN("Journeyman", "Beat Medium ten times.", Category.DIFFICULTY),
        HARD_LABOUR("Hard Labour", "Beat Hard ten times.", Category.DIFFICULTY),
        SLEEPLESS("Sleepless", "Beat Nightmare ten times.", Category.DIFFICULTY),
        CAREER_OFFICER("Career Officer", "Beat US Navy ten times.", Category.DIFFICULTY),
        COMBINED_ARMS("Combined Arms", "Beat All of the US Armed Forces ten times.", Category.DIFFICULTY),
        FLAG_OFFICER("Flag Officer", "Beat every difficulty ten times.", Category.DIFFICULTY),
        UNTOUCHABLE("Untouchable", "Beat US Navy without losing a ship.", Category.DIFFICULTY),
        BEYOND_REASON("Beyond Reason", "Beat All of the US Armed Forces without losing a ship.", Category.DIFFICULTY),
        SCENIC_ROUTE("Scenic Route", "Win in ninety shots or more.", Category.PRECISION),
        ECONOMY_DRIVE("Economy Drive", "Win in under sixty shots.", Category.PRECISION),
        SURGICAL("Surgical", "Win in under forty-five shots.", Category.PRECISION),
        TIGHT_GROUPING("Tight Grouping", "Win in under forty shots.", Category.PRECISION),
        PERFECT_STORM("Perfect Storm", "Win in under thirty-five shots.", Category.PRECISION),
        NEEDLEPOINT("Needlepoint", "Win in under thirty shots.", Category.PRECISION),
        CLAIRVOYANT("Clairvoyant", "Win in under twenty-five shots.", Category.PRECISION),
        IMPOSSIBLE_ODDS("Impossible Odds", "Win in under twenty shots.", Category.PRECISION),
        SEVENTEEN("Seventeen", "Win in seventeen shots, which is the fewest there are.", Category.PRECISION),
        SHARPSHOOTER("Sharpshooter", "Win a game with at least half your shots hitting.", Category.PRECISION),
        MARKSMAN("Marksman", "Win with at least sixty percent of your shots hitting.", Category.PRECISION),
        GUNNERY_OFFICER("Gunnery Officer", "Win with at least seventy percent of your shots hitting.", Category.PRECISION),
        UNCANNY("Uncanny", "Win with at least eighty-five percent of your shots hitting.", Category.PRECISION),
        NOT_ONE_WASTED("Not One Wasted", "Win a game without missing once.", Category.PRECISION),
        STEADY_HAND("Steady Hand", "Reach fifty percent lifetime accuracy over at least fifty games.", Category.PRECISION),
        HAT_TRICK("Hat Trick", "Win three games in a row.", Category.STREAKS),
        UNSINKABLE("Unsinkable", "Win five games in a row.", Category.STREAKS),
        ON_STATION("On Station", "Win ten games in a row.", Category.STREAKS),
        BLOCKADE("Blockade", "Win twenty games in a row.", Category.STREAKS),
        DYNASTY("Dynasty", "Win fifty games in a row.", Category.STREAKS),
        IRON_STREAK("Iron Streak", "Reach a best streak of twenty-five wins.", Category.STREAKS),
        RIGHT_BACK_UP("Right Back Up", "Win the game straight after a loss.", Category.STREAKS),
        UNSCATHED("Not a Scratch", "Win without losing a single ship.", Category.FLEET),
        SCRATCHED_PAINT("Scratched Paint", "Win having lost exactly one ship.", Category.FLEET),
        EVEN_TRADE("Even Trade", "Win having lost four ships.", Category.FLEET),
        LAST_STAND("Last Stand", "Win with one ship left afloat.", Category.FLEET),
        CLOSE_RUN("Close Run Thing", "Win with one ship left afloat and that ship damaged.", Category.FLEET),
        GHOST_FLEET("Ghost Fleet", "Win without the enemy hitting you once.", Category.FLEET),
        THE_BIG_ONE("The Big One", "Win with your carrier never hit.", Category.FLEET),
        LITTLE_SHIP("Little Ship That Could", "Win with your destroyer never hit.", Category.FLEET),
        OPENING_SALVO("Opening Salvo", "Hit with your first shot of a game.", Category.GUNNERY),
        DEAD_RECKONING("Dead Reckoning", "Hit with your first three shots of a game.", Category.GUNNERY),
        ON_A_ROLL("On a Roll", "Land five hits in a row.", Category.GUNNERY),
        LOCKED_ON("Locked On", "Land eight hits in a row.", Category.GUNNERY),
        UNSTOPPABLE("Unstoppable", "Land twelve hits in a row.", Category.GUNNERY),
        WIDE_OF_MARK("Wide of the Mark", "Miss ten shots in a row.", Category.GUNNERY),
        WHERE_ARE_THEY("Where Are They", "Miss fifteen shots in a row.", Category.GUNNERY),
        DOUBLE_STRIKE("Double Strike", "Sink two ships on consecutive shots.", Category.GUNNERY),
        TRIPLE_STRIKE("Triple Strike", "Sink three ships on consecutive shots.", Category.GUNNERY),
        BY_THE_BOOK("By the Book", "Sink the enemy fleet in order, largest ship first.", Category.GUNNERY),
        BACKWARDS("Backwards", "Sink the enemy fleet in order, smallest ship first.", Category.GUNNERY),
        DECAPITATION("Decapitation", "Sink the enemy carrier first.", Category.GUNNERY),
        TOP_LEFT("Top Left", "Hit an enemy ship at A1.", Category.GRID),
        BOTTOM_RIGHT("Bottom Right", "Hit an enemy ship at J10.", Category.GRID),
        ALL_FOUR_CORNERS("All Four Corners", "Fire at all four corners of the grid in one game.", Category.GRID),
        CLEAN_SWEEP("Clean Sweep", "Fire at all ten squares of one row or column in a game.", Category.GRID),
        DEAD_CENTRE("Dead Centre", "Hit an enemy ship in one of the four middle squares.", Category.GRID),
        WARHEAD_READY("Warhead Ready", "Earn a ballistic missile.", Category.ORDNANCE),
        FIRE_IN_THE_HOLE("Fire in the Hole", "Use a ballistic missile.", Category.ORDNANCE),
        OVERKILL("Overkill", "Sink the enemy carrier with a ballistic missile.", Category.ORDNANCE),
        EXPENSIVE_SPLASH("Expensive Splash", "Miss with a ballistic missile.", Category.ORDNANCE),
        HELD_IN_RESERVE("Held in Reserve", "Win with a ballistic missile still unused.", Category.ORDNANCE),
        DECISIVE_STRIKE("Decisive Strike", "Win the game with a ballistic missile.", Category.ORDNANCE),
        OLD_FASHIONED("Old Fashioned", "Beat US Navy or harder without using a ballistic missile.", Category.ORDNANCE),
        HUGGING_THE_COAST("Hugging the Coast", "Win with every ship touching the edge of the grid.", Category.PLACEMENT),
        OPEN_WATER("Open Water", "Win with every ship clear of the edges.", Category.PLACEMENT),
        BROADSIDE("Broadside", "Win with every ship placed horizontally.", Category.PLACEMENT),
        LINE_ASTERN("Line Astern", "Win with every ship placed vertically.", Category.PLACEMENT),
        HUDDLE("Huddle", "Win with every ship inside one half of the grid.", Category.PLACEMENT),
        DISPERSED("Dispersed", "Win with a ship in each quarter of the grid.", Category.PLACEMENT),
        CORNER_OFFICE("Corner Office", "Win with a ship sitting in each of the four corner squares.", Category.PLACEMENT),
        LEAVE_IT_TO_FATE("Leave It to Fate", "Win on a board you randomized.", Category.PLACEMENT),
        HAND_PLACED("Hand Placed", "Win with every ship placed by hand.", Category.PLACEMENT),
        FORTY_TIMES("Forty Times", "Press Randomize forty times in one session.", Category.PLACEMENT),
        SECOND_THOUGHTS("Second Thoughts", "Clear the board ten times.", Category.PLACEMENT),
        HOT_SEAT("Hot Seat", "Finish a local multiplayer game.", Category.MODES),
        PASSING_THE_LAPTOP("Passing the Laptop", "Finish ten local multiplayer games.", Category.MODES),
        TABLE_MANNERS("Table Manners", "Win a local multiplayer game without losing a ship.", Category.MODES),
        HARBOUR_MASTER("Harbour Master", "Host a network game.", Category.MODES),
        GUEST_ABOARD("Guest Aboard", "Join a network game.", Category.MODES),
        SHIP_TO_SHIP("Ship to Ship", "Win a network game.", Category.MODES),
        FLEET_ACTION("Fleet Action", "Play ten network games.", Category.MODES),
        NOTHING_PERSONAL("Nothing Personal", "Win a network game without losing a ship.", Category.MODES),
        BOTH_SIDES("Both Sides", "Host a game and join a game.", Category.MODES),
        WAR_ROOM("War Room", "Choose the Crimson theme.", Category.PRESENTATION),
        DEEP_WATER("Deep Water", "Choose the Abyss theme.", Category.PRESENTATION),
        BLINDS_OPEN("Blinds Open", "Choose the Daylight theme.", Category.PRESENTATION),
        SIGNAL_FLAGS("Signal Flags", "Choose the Signal theme.", Category.PRESENTATION),
        CRY_FOR_HELP("Cry for Help", "Choose the Windows 98 theme.", Category.PRESENTATION),
        LUNA("Luna", "Choose the Windows XP theme.", Category.PRESENTATION),
        AERO("Aero", "Choose the Windows 7 theme.", Category.PRESENTATION),
        CUPERTINO("Cupertino", "Choose the macOS theme.", Category.PRESENTATION),
        INTERIOR_DECORATOR("Interior Decorator", "Choose all nine themes.", Category.PRESENTATION),
        SILENT_RUNNING("Silent Running", "Finish a game with the master volume at zero.", Category.PRESENTATION),
        FULL_BLAST("Full Blast", "Finish a game with every volume slider at maximum.", Category.CURIOSITIES),
        BIG_SCREEN("Big Screen", "Play a game in fullscreen.", Category.CURIOSITIES),
        DISCO("Disco Inferno", "Find the party.", Category.CURIOSITIES),
        DANCING_UNDER_FIRE("Dancing Under Fire", "Start the party during a battle.", Category.CURIOSITIES),
        LEGAL_SCHOLAR("Legal Scholar", "Read the terms of service all the way to the end.", Category.CURIOSITIES),
        KEEPING_RECORDS("Keeping Records", "Open the battle log.", Category.CURIOSITIES),
        CLEAN_SLATE("Clean Slate", "Erase your statistics.", Category.CURIOSITIES),
        AFTER_YOU("After You", "Pass a turn.", Category.CURIOSITIES),
        GRACIOUS("Gracious", "Win a game in which you passed a turn.", Category.CURIOSITIES),
        QUICK_WORK("Quick Work", "Win a game in under three minutes.", Category.CURIOSITIES),
        MIDNIGHT_SORTIE("Midnight Sortie", "Finish a game between midnight and four in the morning.", Category.CURIOSITIES);

        private final String displayName;
        private final String description;
        private final Category category;

        Achievement(String displayName, String description, Category category) {
            this.displayName = displayName;
            this.description = description;
            this.category = category;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public Category getCategory() {
            return category;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final Path FILE =
        Path.of(System.getProperty("user.home"), ".battleshipjava", "achievements.properties");

    private static final String COUNT_PREFIX = "count.";
    private static final String FLAG_PREFIX = "flag.";

    // Counters kept between sessions.
    private static final String BOARDS_CLEARED = "boards.cleared";
    private static final String LOCAL_GAMES = "local.games";
    private static final String NETWORK_GAMES = "network.games";

    // Flags kept between sessions.
    private static final String HOSTED = "hosted";
    private static final String JOINED = "joined";
    private static final String THEME_PREFIX = "theme.";

    private static Achievements instance;

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private final List<Achievement> recentlyUnlocked = new ArrayList<>();
    private final Map<String, Integer> counters = new TreeMap<>();
    private final Set<String> flags = new java.util.TreeSet<>();

    /** Reset every time the game starts, because the clause says "in a single session". */
    private int randomizePressesThisSession;

    private Achievements() {
    }

    public static synchronized Achievements get() {
        if (instance == null) {
            instance = new Achievements();
            instance.load();
        }
        return instance;
    }

    public boolean isUnlocked(Achievement achievement) {
        return unlocked.contains(achievement);
    }

    public int unlockedCount() {
        return unlocked.size();
    }

    public int unlockedCount(Category category) {
        int total = 0;
        for (Achievement achievement : unlocked) {
            if (achievement.getCategory() == category) {
                total++;
            }
        }
        return total;
    }

    public static int totalIn(Category category) {
        int total = 0;
        for (Achievement achievement : Achievement.values()) {
            if (achievement.getCategory() == category) {
                total++;
            }
        }
        return total;
    }

    /** Unlocks one directly. Returns true only the first time. */
    public synchronized boolean unlock(Achievement achievement) {
        if (!unlocked.add(achievement)) {
            return false;
        }
        recentlyUnlocked.add(achievement);
        save();
        return true;
    }

    /** Unlocks it only if the condition holds, which keeps the checks below to one line each. */
    private void unlockIf(boolean condition, Achievement achievement) {
        if (condition) {
            unlock(achievement);
        }
    }

    // ── Things that happen outside a game ───────────────────────────────────

    /** A theme was picked in Settings. Nine of them, and one for having seen them all. */
    public synchronized void themeChosen(Settings.Theme theme) {
        if (theme == null) {
            return;
        }
        raiseFlag(THEME_PREFIX + theme.name());
        switch (theme) {
            case CRIMSON -> unlock(Achievement.WAR_ROOM);
            case ABYSS -> unlock(Achievement.DEEP_WATER);
            case DAYLIGHT -> unlock(Achievement.BLINDS_OPEN);
            case SIGNAL -> unlock(Achievement.SIGNAL_FLAGS);
            case WIN98 -> unlock(Achievement.CRY_FOR_HELP);
            case WINXP -> unlock(Achievement.LUNA);
            case WIN7 -> unlock(Achievement.AERO);
            case MACOS -> unlock(Achievement.CUPERTINO);
            case NAVY -> { /* the default, so seeing it is not an achievement on its own */ }
        }
        for (Settings.Theme candidate : Settings.Theme.values()) {
            if (!flags.contains(FLAG_PREFIX + THEME_PREFIX + candidate.name())) {
                return;
            }
        }
        unlock(Achievement.INTERIOR_DECORATOR);
    }

    /** Randomize was pressed. Clause 3.4 of the terms takes an interest at forty. */
    public synchronized void boardRandomized() {
        randomizePressesThisSession++;
        unlockIf(randomizePressesThisSession >= 40, Achievement.FORTY_TIMES);
    }

    public synchronized void boardCleared() {
        unlockIf(bump(BOARDS_CLEARED) >= 10, Achievement.SECOND_THOUGHTS);
    }

    public synchronized void battleLogOpened() {
        unlock(Achievement.KEEPING_RECORDS);
    }

    public synchronized void statisticsErased() {
        unlock(Achievement.CLEAN_SLATE);
    }

    public synchronized void turnPassed() {
        unlock(Achievement.AFTER_YOU);
    }

    public synchronized void partyStarted(boolean duringBattle) {
        unlock(Achievement.DISCO);
        unlockIf(duringBattle, Achievement.DANCING_UNDER_FIRE);
    }

    public synchronized void gameHosted() {
        raiseFlag(HOSTED);
        unlock(Achievement.HARBOUR_MASTER);
        unlockIf(flags.contains(FLAG_PREFIX + JOINED), Achievement.BOTH_SIDES);
    }

    public synchronized void gameJoined() {
        raiseFlag(JOINED);
        unlock(Achievement.GUEST_ABOARD);
        unlockIf(flags.contains(FLAG_PREFIX + HOSTED), Achievement.BOTH_SIDES);
    }

    // ── A finished game ─────────────────────────────────────────────────────

    /**
     * Works out what a finished game earned. Call this after the statistics for that game have
     * been recorded, since the lifetime and streak checks read from them.
     */
    public synchronized void evaluate(GameRecord record) {
        if (record == null) {
            return;
        }
        evaluateSurroundings(record);
        switch (record.mode) {
            case SINGLEPLAYER -> {
                evaluateLifetime(record);
                evaluateOneGame(record);
            }
            case NETWORK -> {
                evaluateNetwork(record);
                evaluateOneGame(record);
            }
            case LOCAL -> evaluateLocal(record);
        }
    }

    /** True of the room rather than the game, so it counts however you were playing. */
    private void evaluateSurroundings(GameRecord record) {
        Settings settings = Settings.get();
        unlockIf(settings.getMasterVolume() == 0, Achievement.SILENT_RUNNING);
        unlockIf(settings.getMasterVolume() >= 1 && settings.getMusicVolume() >= 1
            && settings.getEffectsVolume() >= 1, Achievement.FULL_BLAST);
        unlockIf(settings.getDisplayMode() != Settings.DisplayMode.WINDOWED, Achievement.BIG_SCREEN);
        unlockIf(record.finishedHour >= 0 && record.finishedHour < 4, Achievement.MIDNIGHT_SORTIE);
    }

    /** The tallies, the difficulty ladder and the streaks. Single player only. */
    private void evaluateLifetime(GameRecord record) {
        if (record.difficulty == null) {
            return;
        }
        Statistics statistics = Statistics.get();

        int played = statistics.getGamesPlayed();
        unlockIf(played >= 10, Achievement.FEET_WET);
        unlockIf(played >= 50, Achievement.VETERAN);
        unlockIf(played >= 100, Achievement.HUNDRED_SORTIES);
        unlockIf(played >= 500, Achievement.OLD_SALT);
        unlockIf(played >= 1000, Achievement.THOUSAND_YARD);

        int lost = statistics.getGamesLost();
        unlockIf(lost >= 1, Achievement.BAPTISM);
        unlockIf(lost >= 50, Achievement.THICK_SKIN);

        unlockIf(statistics.getAccuracy() >= 0.5 && played >= 50, Achievement.STEADY_HAND);

        if (!record.won) {
            return;
        }

        int won = statistics.getGamesWon();
        unlock(Achievement.FIRST_BLOOD);
        unlockIf(won >= 5, Achievement.SHAKEDOWN);
        unlockIf(won >= 10, Achievement.TEN_HULLS);
        unlockIf(won >= 25, Achievement.QUARTERDECK);
        unlockIf(won >= 50, Achievement.FIFTY_FATHOMS);
        unlockIf(won >= 100, Achievement.CENTURION);
        unlockIf(won >= 250, Achievement.FLEET_ADMIRAL);

        int streak = statistics.getCurrentStreak();
        unlockIf(streak >= 3, Achievement.HAT_TRICK);
        unlockIf(streak >= 5, Achievement.UNSINKABLE);
        unlockIf(streak >= 10, Achievement.ON_STATION);
        unlockIf(streak >= 20, Achievement.BLOCKADE);
        unlockIf(streak >= 50, Achievement.DYNASTY);
        unlockIf(statistics.getLongestStreak() >= 25, Achievement.IRON_STREAK);
        // A streak of exactly one, on anything but your first game, means the last one was a loss.
        unlockIf(streak == 1 && played > 1, Achievement.RIGHT_BACK_UP);

        switch (record.difficulty) {
            case EASY -> unlock(Achievement.SEA_TRIALS);
            case MEDIUM -> unlock(Achievement.BLUE_WATER);
            case HARD -> unlock(Achievement.HARD_APORT);
            case NIGHTMARE -> unlock(Achievement.NIGHT_WATCH);
            case US_NAVY -> unlock(Achievement.ADMIRAL);
            case ARMED_FORCES -> unlock(Achievement.JOINT_CHIEFS);
        }
        unlockIf(statistics.recordFor(Difficulty.EASY).won() >= 10, Achievement.MILK_RUN);
        unlockIf(statistics.recordFor(Difficulty.MEDIUM).won() >= 10, Achievement.JOURNEYMAN);
        unlockIf(statistics.recordFor(Difficulty.HARD).won() >= 10, Achievement.HARD_LABOUR);
        unlockIf(statistics.recordFor(Difficulty.NIGHTMARE).won() >= 10, Achievement.SLEEPLESS);
        unlockIf(statistics.recordFor(Difficulty.US_NAVY).won() >= 10, Achievement.CAREER_OFFICER);
        unlockIf(statistics.recordFor(Difficulty.ARMED_FORCES).won() >= 10, Achievement.COMBINED_ARMS);
        unlockIf(beatenEveryDifficulty(statistics, 1), Achievement.ALL_HANDS);
        unlockIf(beatenEveryDifficulty(statistics, 10), Achievement.FLAG_OFFICER);

        unlockIf(record.difficulty == Difficulty.US_NAVY && record.shipsLost == 0, Achievement.UNTOUCHABLE);
        unlockIf(record.difficulty == Difficulty.ARMED_FORCES && record.shipsLost == 0, Achievement.BEYOND_REASON);
        boolean topTwo = record.difficulty == Difficulty.US_NAVY || record.difficulty == Difficulty.ARMED_FORCES;
        unlockIf(topTwo && !record.ballisticUsed, Achievement.OLD_FASHIONED);
    }

    /** Everything one player can be judged on from the game just played. */
    private void evaluateOneGame(GameRecord record) {
        // Gunnery and the grid are feats during play, so a loss still counts.
        unlockIf(record.firstShotHit, Achievement.OPENING_SALVO);
        unlockIf(record.firstThreeHits, Achievement.DEAD_RECKONING);
        unlockIf(record.longestHitStreak >= 5, Achievement.ON_A_ROLL);
        unlockIf(record.longestHitStreak >= 8, Achievement.LOCKED_ON);
        unlockIf(record.longestHitStreak >= 12, Achievement.UNSTOPPABLE);
        unlockIf(record.longestMissStreak >= 10, Achievement.WIDE_OF_MARK);
        unlockIf(record.longestMissStreak >= 15, Achievement.WHERE_ARE_THEY);
        unlockIf(record.mostConsecutiveSinks >= 2, Achievement.DOUBLE_STRIKE);
        unlockIf(record.mostConsecutiveSinks >= 3, Achievement.TRIPLE_STRIKE);
        unlockIf(record.sunkLargestFirst(), Achievement.BY_THE_BOOK);
        unlockIf(record.sunkSmallestFirst(), Achievement.BACKWARDS);
        unlockIf(!record.sinkOrder.isEmpty() && record.sinkOrder.get(0) == ShipType.CARRIER, Achievement.DECAPITATION);

        unlockIf(record.hitTopLeft, Achievement.TOP_LEFT);
        unlockIf(record.hitBottomRight, Achievement.BOTTOM_RIGHT);
        unlockIf(record.firedEveryCorner, Achievement.ALL_FOUR_CORNERS);
        unlockIf(record.sweptALine, Achievement.CLEAN_SWEEP);
        unlockIf(record.hitDeadCentre, Achievement.DEAD_CENTRE);

        unlockIf(record.ballisticEarned, Achievement.WARHEAD_READY);
        unlockIf(record.ballisticUsed, Achievement.FIRE_IN_THE_HOLE);
        unlockIf(record.ballisticSankCarrier, Achievement.OVERKILL);
        unlockIf(record.ballisticMissed, Achievement.EXPENSIVE_SPLASH);

        if (!record.won) {
            return;
        }

        unlockIf(record.shots >= 90, Achievement.SCENIC_ROUTE);
        unlockIf(record.shots < 60, Achievement.ECONOMY_DRIVE);
        unlockIf(record.shots < 45, Achievement.SURGICAL);
        unlockIf(record.shots < 40, Achievement.TIGHT_GROUPING);
        unlockIf(record.shots < 35, Achievement.PERFECT_STORM);
        unlockIf(record.shots < 30, Achievement.NEEDLEPOINT);
        unlockIf(record.shots < 25, Achievement.CLAIRVOYANT);
        unlockIf(record.shots < 20, Achievement.IMPOSSIBLE_ODDS);
        unlockIf(record.shots == 17, Achievement.SEVENTEEN);

        double accuracy = record.accuracy();
        unlockIf(accuracy >= 0.5, Achievement.SHARPSHOOTER);
        unlockIf(accuracy >= 0.6, Achievement.MARKSMAN);
        unlockIf(accuracy >= 0.7, Achievement.GUNNERY_OFFICER);
        unlockIf(accuracy >= 0.85, Achievement.UNCANNY);
        unlockIf(record.shots > 0 && record.hits == record.shots, Achievement.NOT_ONE_WASTED);

        unlockIf(record.shipsLost == 0, Achievement.UNSCATHED);
        unlockIf(record.shipsLost == 1, Achievement.SCRATCHED_PAINT);
        unlockIf(record.shipsLost == 4, Achievement.EVEN_TRADE);
        unlockIf(record.shipsRemaining == 1, Achievement.LAST_STAND);
        unlockIf(record.shipsRemaining == 1 && record.lastShipDamaged, Achievement.CLOSE_RUN);
        unlockIf(record.enemyHitsTaken == 0, Achievement.GHOST_FLEET);
        unlockIf(!record.carrierEverHit, Achievement.THE_BIG_ONE);
        unlockIf(!record.destroyerEverHit, Achievement.LITTLE_SHIP);

        unlockIf(record.ballisticEarned && !record.ballisticUsed, Achievement.HELD_IN_RESERVE);
        unlockIf(record.wonWithBallistic, Achievement.DECISIVE_STRIKE);

        unlockIf(record.everyShipOnEdge, Achievement.HUGGING_THE_COAST);
        unlockIf(record.noShipOnEdge, Achievement.OPEN_WATER);
        unlockIf(record.allHorizontal, Achievement.BROADSIDE);
        unlockIf(record.allVertical, Achievement.LINE_ASTERN);
        unlockIf(record.withinOneHalf, Achievement.HUDDLE);
        unlockIf(record.shipInEveryQuarter, Achievement.DISPERSED);
        unlockIf(record.shipInEveryCorner, Achievement.CORNER_OFFICE);
        unlockIf(record.randomizedBoard, Achievement.LEAVE_IT_TO_FATE);
        unlockIf(!record.randomizedBoard, Achievement.HAND_PLACED);

        unlockIf(record.turnsPassed > 0, Achievement.GRACIOUS);
        unlockIf(record.durationMillis > 0 && record.durationMillis < 180_000, Achievement.QUICK_WORK);
    }

    private void evaluateLocal(GameRecord record) {
        unlock(Achievement.HOT_SEAT);
        unlockIf(bump(LOCAL_GAMES) >= 10, Achievement.PASSING_THE_LAPTOP);
        unlockIf(record.won && record.shipsLost == 0, Achievement.TABLE_MANNERS);
    }

    private void evaluateNetwork(GameRecord record) {
        unlockIf(bump(NETWORK_GAMES) >= 10, Achievement.FLEET_ACTION);
        unlockIf(record.won, Achievement.SHIP_TO_SHIP);
        unlockIf(record.won && record.shipsLost == 0, Achievement.NOTHING_PERSONAL);
    }

    private static boolean beatenEveryDifficulty(Statistics statistics, int wins) {
        for (Difficulty difficulty : Difficulty.values()) {
            if (statistics.recordFor(difficulty).won() < wins) {
                return false;
            }
        }
        return true;
    }

    /** Hands back anything unlocked since this was last called, and clears the list. */
    public synchronized List<Achievement> takeRecentlyUnlocked() {
        List<Achievement> taken = List.copyOf(recentlyUnlocked);
        recentlyUnlocked.clear();
        return taken;
    }

    public synchronized void reset() {
        unlocked.clear();
        recentlyUnlocked.clear();
        counters.clear();
        flags.clear();
        randomizePressesThisSession = 0;
        save();
    }

    // ── Counters and flags ──────────────────────────────────────────────────

    private int bump(String key) {
        int next = counters.getOrDefault(key, 0) + 1;
        counters.put(key, next);
        save();
        return next;
    }

    private void raiseFlag(String key) {
        if (flags.add(FLAG_PREFIX + key)) {
            save();
        }
    }

    // ── Storage ─────────────────────────────────────────────────────────────

    private void load() {
        if (!Files.isReadable(FILE)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            properties.load(in);
        } catch (IOException exception) {
            return;
        }
        for (Achievement achievement : Achievement.values()) {
            if (Boolean.parseBoolean(properties.getProperty(achievement.name(), "false"))) {
                unlocked.add(achievement);
            }
        }
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(COUNT_PREFIX)) {
                try {
                    counters.put(name.substring(COUNT_PREFIX.length()), Integer.parseInt(properties.getProperty(name)));
                } catch (NumberFormatException ignored) {
                    // A counter we cannot read starts again from zero.
                }
            } else if (name.startsWith(FLAG_PREFIX) && Boolean.parseBoolean(properties.getProperty(name))) {
                flags.add(name);
            }
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (Achievement achievement : Achievement.values()) {
            properties.setProperty(achievement.name(), Boolean.toString(unlocked.contains(achievement)));
        }
        Map<String, Integer> ordered = new LinkedHashMap<>(counters);
        for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
            properties.setProperty(COUNT_PREFIX + entry.getKey(), Integer.toString(entry.getValue()));
        }
        for (String flag : flags) {
            properties.setProperty(flag, "true");
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                properties.store(out, "BattleshipJava achievements");
            }
        } catch (IOException ignored) {
            // Not worth interrupting a game over.
        }
    }
}
