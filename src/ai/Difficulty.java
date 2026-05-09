package ai;

public enum Difficulty {
    EASY("Easy", "Random shots only. Best for beginners."),
    MEDIUM("Medium", "Targets adjacent tiles after a hit."),
    HARD("Hard", "Tracks hit direction to sink ships efficiently."),
    NIGHTMARE("Nightmare", "Parity hunting with precision line targeting. Nearly unbeatable."),
    US_NAVY("US Navy", "Probability density targeting. Calculates the most likely ship location every turn. Plays near-perfectly."),
    ARMED_FORCES("All of the US Armed Forces", "Every branch at once. Sweeps on the tightest pattern that cannot miss a ship, counts every placement still possible, and locks on hard the moment it draws blood. It never sees your board. It just never wastes a shot.");

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
