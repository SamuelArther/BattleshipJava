package game;

public class AttackOutcome {
    private final AttackResult result;
    private final boolean sunkShip;
    private final boolean gameOver;
    private final ShipType shipType;

    public AttackOutcome(AttackResult result, boolean sunkShip, boolean gameOver, ShipType shipType) {
        this.result = result;
        this.sunkShip = sunkShip;
        this.gameOver = gameOver;
        this.shipType = shipType;
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

    public ShipType getShipType() {
        return shipType;
    }
}
