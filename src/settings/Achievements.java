package settings;

import ai.Difficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Things worth doing, and whether you have done them.
 *
 * Everything here is checked against what actually happened in a game rather than being handed
 * out for turning up. Nothing unlocks twice, and nothing unlocks retroactively from statistics
 * that were already banked, because an achievement you never saw is not much of a reward.
 */
public final class Achievements {

    public enum Achievement {
        FIRST_BLOOD("First Blood", "Win a game."),
        SHARPSHOOTER("Sharpshooter", "Win a game with at least half your shots hitting."),
        SURGICAL("Surgical", "Win in under 45 shots."),
        PERFECT_STORM("Perfect Storm", "Win in under 35 shots."),
        UNSCATHED("Not a Scratch", "Win without losing a single ship."),
        ADMIRAL("Admiral", "Beat US Navy."),
        JOINT_CHIEFS("Joint Chiefs", "Beat All of the US Armed Forces."),
        ALL_HANDS("All Hands", "Beat every difficulty at least once."),
        UNSINKABLE("Unsinkable", "Win five games in a row."),
        VETERAN("Veteran", "Play fifty games."),
        DISCO("Disco Inferno", "Find the party."),
        LEGAL_SCHOLAR("Legal Scholar", "Read the terms of service all the way to the end.");

        private final String displayName;
        private final String description;

        Achievement(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** What one finished game looked like, which is everything the checks below need. */
    public record GameResult(Difficulty difficulty, boolean won, int shots, int hits, int shipsLost) {
        public double accuracy() {
            return shots == 0 ? 0 : (double) hits / shots;
        }
    }

    private static final Path FILE =
        Path.of(System.getProperty("user.home"), ".battleshipjava", "achievements.properties");

    private static Achievements instance;

    private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private final List<Achievement> recentlyUnlocked = new ArrayList<>();

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

    /** Unlocks one directly. Returns true only the first time. */
    public synchronized boolean unlock(Achievement achievement) {
        if (!unlocked.add(achievement)) {
            return false;
        }
        recentlyUnlocked.add(achievement);
        save();
        return true;
    }

    /**
     * Works out what a finished game earned. Call this after the statistics for that game have
     * been recorded, since the streak and total checks read from them.
     */
    public synchronized void evaluate(GameResult result) {
        if (result.difficulty() == null) {
            return;
        }
        Statistics statistics = Statistics.get();

        if (statistics.getGamesPlayed() >= 50) {
            unlock(Achievement.VETERAN);
        }
        if (!result.won()) {
            return;
        }

        unlock(Achievement.FIRST_BLOOD);
        if (result.accuracy() >= 0.5) {
            unlock(Achievement.SHARPSHOOTER);
        }
        if (result.shots() < 45) {
            unlock(Achievement.SURGICAL);
        }
        if (result.shots() < 35) {
            unlock(Achievement.PERFECT_STORM);
        }
        if (result.shipsLost() == 0) {
            unlock(Achievement.UNSCATHED);
        }
        if (result.difficulty() == Difficulty.US_NAVY) {
            unlock(Achievement.ADMIRAL);
        }
        if (result.difficulty() == Difficulty.ARMED_FORCES) {
            unlock(Achievement.JOINT_CHIEFS);
        }
        if (statistics.getCurrentStreak() >= 5) {
            unlock(Achievement.UNSINKABLE);
        }
        if (beatenEveryDifficulty(statistics)) {
            unlock(Achievement.ALL_HANDS);
        }
    }

    private static boolean beatenEveryDifficulty(Statistics statistics) {
        for (Difficulty difficulty : Difficulty.values()) {
            if (statistics.recordFor(difficulty).won() == 0) {
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
        save();
    }

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
    }

    private void save() {
        Properties properties = new Properties();
        for (Achievement achievement : Achievement.values()) {
            properties.setProperty(achievement.name(), Boolean.toString(unlocked.contains(achievement)));
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
