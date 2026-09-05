package settings;

import ai.Difficulty;
import game.Board;
import game.Coordinate;
import game.Ship;
import game.ShipType;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything one finished game can be judged on.
 *
 * The scene fills this in as the game runs and hands it to {@link Achievements} at the end.
 * Nothing here is guessed or inferred later: if a field says a shot hit, a shot hit, and if a
 * field is false it is because the thing did not happen rather than because nobody was watching.
 *
 * A few of the fields cannot be known in every mode. A network peer is told only hit or miss,
 * so nothing about sinking or about the enemy fleet's order is filled in there, and the
 * achievements that depend on those simply do not come up. That is the honest outcome: they
 * are not awarded on a guess.
 */
public final class GameRecord {

    /** Which of the three ways of playing produced this record. */
    public enum Mode { SINGLEPLAYER, LOCAL, NETWORK }

    public Mode mode = Mode.SINGLEPLAYER;
    public Difficulty difficulty;
    public boolean won;

    // What you fired, and what it cost you.
    public int shots;
    public int hits;
    public int shipsLost;
    public int shipsRemaining = 5;
    public boolean lastShipDamaged;
    public int enemyHitsTaken;
    public boolean carrierEverHit;
    public boolean destroyerEverHit;

    // How the shooting went.
    public boolean firstShotHit;
    public boolean firstThreeHits;
    public int longestHitStreak;
    public int longestMissStreak;
    public int mostConsecutiveSinks;
    public final List<ShipType> sinkOrder = new ArrayList<>();

    // Where on the grid it went.
    public boolean hitTopLeft;
    public boolean hitBottomRight;
    public boolean hitDeadCentre;
    public boolean firedEveryCorner;
    public boolean sweptALine;

    // The perk.
    public boolean ballisticEarned;
    public boolean ballisticUsed;
    public boolean ballisticMissed;
    public boolean ballisticSankCarrier;
    public boolean wonWithBallistic;

    // How your own fleet was laid out.
    public boolean everyShipOnEdge;
    public boolean noShipOnEdge;
    public boolean allHorizontal;
    public boolean allVertical;
    public boolean withinOneHalf;
    public boolean shipInEveryQuarter;
    public boolean shipInEveryCorner;
    public boolean randomizedBoard;

    // The rest.
    public int turnsPassed;
    public long durationMillis;
    public int finishedHour;

    public double accuracy() {
        return shots == 0 ? 0 : (double) hits / shots;
    }

    /**
     * Reads the shape of your own fleet off the board you played with.
     *
     * Done at the end rather than during placement so it describes the fleet you actually
     * fought with, including a board you randomized and then never touched.
     */
    public void describeFleet(Board board) {
        List<Ship> ships = board.getShips();
        if (ships.size() != ShipType.values().length) {
            return;
        }
        randomizedBoard = board.wasRandomized();

        everyShipOnEdge = true;
        noShipOnEdge = true;
        allHorizontal = true;
        allVertical = true;

        boolean[] quarterUsed = new boolean[4];
        boolean topHalf = true, bottomHalf = true, leftHalf = true, rightHalf = true;

        for (Ship ship : ships) {
            boolean touchesEdge = false;
            int firstX = ship.getCoordinates().get(0).x();
            int firstY = ship.getCoordinates().get(0).y();

            for (Coordinate coordinate : ship.getCoordinates()) {
                int x = coordinate.x();
                int y = coordinate.y();
                if (x == 0 || y == 0 || x == Board.SIZE - 1 || y == Board.SIZE - 1) {
                    touchesEdge = true;
                    noShipOnEdge = false;
                }
                if (x != firstX) {
                    allVertical = false;
                }
                if (y != firstY) {
                    allHorizontal = false;
                }
                quarterUsed[(y < Board.SIZE / 2 ? 0 : 2) + (x < Board.SIZE / 2 ? 0 : 1)] = true;
                if (y >= Board.SIZE / 2) topHalf = false;
                if (y < Board.SIZE / 2) bottomHalf = false;
                if (x >= Board.SIZE / 2) leftHalf = false;
                if (x < Board.SIZE / 2) rightHalf = false;
            }
            if (!touchesEdge) {
                everyShipOnEdge = false;
            }
        }

        withinOneHalf = topHalf || bottomHalf || leftHalf || rightHalf;
        shipInEveryQuarter = quarterUsed[0] && quarterUsed[1] && quarterUsed[2] && quarterUsed[3];

        int last = Board.SIZE - 1;
        shipInEveryCorner = board.getTile(0, 0).hasShip()
            && board.getTile(last, 0).hasShip()
            && board.getTile(0, last).hasShip()
            && board.getTile(last, last).hasShip();
    }

    /** True if the enemy fleet went down largest first, allowing for the two ships of three. */
    public boolean sunkLargestFirst() {
        return sunkInSizeOrder(true);
    }

    /** True if the enemy fleet went down smallest first. */
    public boolean sunkSmallestFirst() {
        return sunkInSizeOrder(false);
    }

    private boolean sunkInSizeOrder(boolean descending) {
        if (sinkOrder.size() != ShipType.values().length) {
            return false;
        }
        for (int i = 1; i < sinkOrder.size(); i++) {
            int previous = sinkOrder.get(i - 1).getSize();
            int current = sinkOrder.get(i).getSize();
            if (descending ? current > previous : current < previous) {
                return false;
            }
        }
        return true;
    }
}
