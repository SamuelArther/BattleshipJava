package ai;

public enum Difficulty {
    LEVEL_1("Level 1", "Random shots only. Best for a relaxed game."),
    LEVEL_2("Level 2", "Targets adjacent tiles after a hit. More challenging.");

    private final String displayName;
    private final String description;

    Difficulty(String displayName, String description) {
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
