package game;

public class AttackOutcome {
    private final AttackResult result;
    private final boolean sunkShip;
    private final boolean gameOver;
    private final Ship ship;

    public AttackOutcome(AttackResult result, boolean sunkShip, boolean gameOver, Ship ship) {
        this.result = result;
        this.sunkShip = sunkShip;
        this.gameOver = gameOver;
        this.ship = ship;
    }

    public AttackResult getResult() {
        return result;
    }

    public boolean isSunkShip() {
        return sunkShip;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** The ship at the attacked tile, or null if there is none. */
    public Ship getShip() {
        return ship;
    }

    public ShipType getShipType() {
        return ship == null ? null : ship.getType();
    }
}
