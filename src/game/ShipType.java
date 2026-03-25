package game;

public enum ShipType {
    CARRIER("Carrier", 5),
    BATTLESHIP("Battleship", 4),
    CRUISER("Cruiser", 3),
    SUBMARINE("Submarine", 3),
    DESTROYER("Destroyer", 2);

    private final String displayName;
    private final int size;

    ShipType(String displayName, int size) {
        this.displayName = displayName;
        this.size = size;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSize() {
        return size;
    }
}
