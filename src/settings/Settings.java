package settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The player's video and audio preferences, remembered between sessions.
 *
 * Stored as a small properties file under the user's home directory rather than beside the
 * game, so an installed copy in a read-only folder can still save settings, and so the
 * settings survive replacing the game with a newer build.
 */
public final class Settings {

    /** How the game window presents itself. */
    public enum DisplayMode {
        WINDOWED("Windowed", "A normal resizable window with a title bar."),
        BORDERLESS("Borderless fullscreen", "Fills the screen with no border. Alt-Tab stays instant."),
        FULLSCREEN("Exclusive fullscreen", "True fullscreen. Press F11 or Esc to leave.");

        private final String displayName;
        private final String description;

        DisplayMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** A window size offered in the settings screen. */
    public record WindowSize(int width, int height) {
        @Override
        public String toString() {
            return width + " x " + height;
        }
    }

    public static final WindowSize[] WINDOW_SIZES = {
        new WindowSize(1100, 760),
        new WindowSize(1280, 800),
        new WindowSize(1440, 900),
        new WindowSize(1600, 900),
        new WindowSize(1920, 1080)
    };

    private static final Path FILE =
        Path.of(System.getProperty("user.home"), ".battleshipjava", "settings.properties");

    private static Settings instance;

    private double masterVolume = 1.0;
    private double musicVolume = 0.8;
    private double effectsVolume = 1.0;
    private DisplayMode displayMode = DisplayMode.WINDOWED;
    private int windowWidth = 1280;
    private int windowHeight = 800;

    private Settings() {
    }

    public static synchronized Settings get() {
        if (instance == null) {
            instance = new Settings();
            instance.load();
        }
        return instance;
    }

    public double getMasterVolume()  { return masterVolume; }
    public double getMusicVolume()   { return musicVolume; }
    public double getEffectsVolume() { return effectsVolume; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public int getWindowWidth()  { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }

    public void setMasterVolume(double value)  { masterVolume = clamp(value); }
    public void setMusicVolume(double value)   { musicVolume = clamp(value); }
    public void setEffectsVolume(double value) { effectsVolume = clamp(value); }
    public void setDisplayMode(DisplayMode value) { displayMode = value == null ? DisplayMode.WINDOWED : value; }

    public void setWindowSize(WindowSize size) {
        if (size != null) {
            windowWidth = size.width();
            windowHeight = size.height();
        }
    }

    public WindowSize getWindowSize() {
        return new WindowSize(windowWidth, windowHeight);
    }

    /** The volume a music track should play at, once both sliders are taken into account. */
    public double musicLevel(double trackVolume) {
        return clamp(trackVolume * musicVolume * masterVolume);
    }

    /** The volume a sound effect should play at, once both sliders are taken into account. */
    public double effectsLevel(double trackVolume) {
        return clamp(trackVolume * effectsVolume * masterVolume);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public void load() {
        if (!Files.isReadable(FILE)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            properties.load(in);
        } catch (IOException exception) {
            return; // A settings file we cannot read is not worth failing to start over.
        }
        masterVolume  = readDouble(properties, "volume.master", masterVolume);
        musicVolume   = readDouble(properties, "volume.music", musicVolume);
        effectsVolume = readDouble(properties, "volume.effects", effectsVolume);
        windowWidth   = readInt(properties, "window.width", windowWidth);
        windowHeight  = readInt(properties, "window.height", windowHeight);
        try {
            displayMode = DisplayMode.valueOf(properties.getProperty("display.mode", displayMode.name()));
        } catch (IllegalArgumentException ignored) {
            displayMode = DisplayMode.WINDOWED;
        }
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("volume.master", Double.toString(masterVolume));
        properties.setProperty("volume.music", Double.toString(musicVolume));
        properties.setProperty("volume.effects", Double.toString(effectsVolume));
        properties.setProperty("display.mode", displayMode.name());
        properties.setProperty("window.width", Integer.toString(windowWidth));
        properties.setProperty("window.height", Integer.toString(windowHeight));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                properties.store(out, "BattleshipJava settings");
            }
        } catch (IOException ignored) {
            // Losing the preferences is a nuisance, not a reason to interrupt the game.
        }
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return clamp(Double.parseDouble(properties.getProperty(key, Double.toString(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
