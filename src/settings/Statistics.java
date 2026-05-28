package settings;

import ai.Difficulty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * A running record of every game played against the computer.
 *
 * Only single-player games are counted. A local hot-seat game has no single result that belongs
 * to the person whose statistics these are, and a network game is decided as much by the other
 * player as by this one, so neither would say anything useful about how well you play.
 *
 * Kept beside the settings, under the user's home directory, for the same reasons.
 */
public final class Statistics {

    /** What one difficulty's record looks like. */
    public record Record(int played, int won, int bestShots) {
        public int lost() {
            return played - won;
        }

        public double winRate() {
            return played == 0 ? 0 : (double) won / played;
        }

        public boolean hasBest() {
            return bestShots > 0;
        }
    }

    private static final Path FILE =
        Path.of(System.getProperty("user.home"), ".battleshipjava", "statistics.properties");

    private static Statistics instance;

    private final Map<Difficulty, int[]> records = new EnumMap<>(Difficulty.class);
    private long shotsFired;
    private long shotsHit;
    private int currentStreak;
    private int longestStreak;

    private Statistics() {
        for (Difficulty difficulty : Difficulty.values()) {
            records.put(difficulty, new int[] {0, 0, 0}); // played, won, bestShots
        }
    }

    public static synchronized Statistics get() {
        if (instance == null) {
            instance = new Statistics();
            instance.load();
        }
        return instance;
    }

    /**
     * Records a finished single-player game.
     *
     * @param shotsThisGame how many shots the player fired, and how many of them landed
     */
    public synchronized void recordGame(Difficulty difficulty, boolean won, int shotsThisGame, int hitsThisGame) {
        if (difficulty == null) {
            return;
        }
        int[] record = records.get(difficulty);
        record[0]++;
        if (won) {
            record[1]++;
            if (record[2] == 0 || shotsThisGame < record[2]) {
                record[2] = shotsThisGame;
            }
            currentStreak++;
            longestStreak = Math.max(longestStreak, currentStreak);
        } else {
            currentStreak = 0;
        }
        shotsFired += shotsThisGame;
        shotsHit += hitsThisGame;
        save();
    }

    public Record recordFor(Difficulty difficulty) {
        int[] record = records.get(difficulty);
        return new Record(record[0], record[1], record[2]);
    }

    public int getGamesPlayed() {
        int total = 0;
        for (int[] record : records.values()) {
            total += record[0];
        }
        return total;
    }

    public int getGamesWon() {
        int total = 0;
        for (int[] record : records.values()) {
            total += record[1];
        }
        return total;
    }

    public int getGamesLost() {
        return getGamesPlayed() - getGamesWon();
    }

    public double getWinRate() {
        int played = getGamesPlayed();
        return played == 0 ? 0 : (double) getGamesWon() / played;
    }

    public long getShotsFired() {
        return shotsFired;
    }

    public long getShotsHit() {
        return shotsHit;
    }

    public double getAccuracy() {
        return shotsFired == 0 ? 0 : (double) shotsHit / shotsFired;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    /** The hardest difficulty this player has ever beaten, or null if none yet. */
    public Difficulty getHighestCleared() {
        Difficulty best = null;
        for (Difficulty difficulty : Difficulty.values()) {
            if (records.get(difficulty)[1] > 0) {
                best = difficulty;
            }
        }
        return best;
    }

    public synchronized void reset() {
        for (int[] record : records.values()) {
            record[0] = 0;
            record[1] = 0;
            record[2] = 0;
        }
        shotsFired = 0;
        shotsHit = 0;
        currentStreak = 0;
        longestStreak = 0;
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
        shotsFired = readLong(properties, "shots.fired");
        shotsHit = readLong(properties, "shots.hit");
        currentStreak = (int) readLong(properties, "streak.current");
        longestStreak = (int) readLong(properties, "streak.longest");
        for (Difficulty difficulty : Difficulty.values()) {
            int[] record = records.get(difficulty);
            String key = difficulty.name().toLowerCase();
            record[0] = (int) readLong(properties, key + ".played");
            record[1] = (int) readLong(properties, key + ".won");
            record[2] = (int) readLong(properties, key + ".best");
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("shots.fired", Long.toString(shotsFired));
        properties.setProperty("shots.hit", Long.toString(shotsHit));
        properties.setProperty("streak.current", Integer.toString(currentStreak));
        properties.setProperty("streak.longest", Integer.toString(longestStreak));
        for (Difficulty difficulty : Difficulty.values()) {
            int[] record = records.get(difficulty);
            String key = difficulty.name().toLowerCase();
            properties.setProperty(key + ".played", Integer.toString(record[0]));
            properties.setProperty(key + ".won", Integer.toString(record[1]));
            properties.setProperty(key + ".best", Integer.toString(record[2]));
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                properties.store(out, "BattleshipJava statistics");
            }
        } catch (IOException ignored) {
            // Losing a game's record is not a reason to interrupt the game.
        }
    }

    private static long readLong(Properties properties, String key) {
        try {
            return Long.parseLong(properties.getProperty(key, "0"));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
