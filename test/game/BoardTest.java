package game;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    void shipsMustFitOnTheGrid() {
        Board board = new Board();
        assertTrue(board.canPlaceShip(ShipType.CARRIER, 5, 0, Orientation.HORIZONTAL));
        assertFalse(board.canPlaceShip(ShipType.CARRIER, 6, 0, Orientation.HORIZONTAL));
        assertTrue(board.canPlaceShip(ShipType.CARRIER, 0, 5, Orientation.VERTICAL));
        assertFalse(board.canPlaceShip(ShipType.CARRIER, 0, 6, Orientation.VERTICAL));
        assertFalse(board.canPlaceShip(ShipType.DESTROYER, -1, 0, Orientation.HORIZONTAL));
    }

    @Test
    void shipsCannotOverlap() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));
        assertFalse(board.canPlaceShip(ShipType.DESTROYER, 4, 0, Orientation.VERTICAL));
    }

    @Test
    void shipsCannotTouchEvenDiagonally() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL)); // A1 to E1
        assertFalse(board.canPlaceShip(ShipType.DESTROYER, 0, 1, Orientation.HORIZONTAL)); // directly below
        assertFalse(board.canPlaceShip(ShipType.DESTROYER, 5, 1, Orientation.HORIZONTAL)); // diagonal corner
        assertFalse(board.canPlaceShip(ShipType.DESTROYER, 5, 0, Orientation.HORIZONTAL)); // end to end
        assertTrue(board.canPlaceShip(ShipType.DESTROYER, 0, 2, Orientation.HORIZONTAL));  // one row of water between
        assertTrue(board.canPlaceShip(ShipType.DESTROYER, 6, 0, Orientation.HORIZONTAL));  // one column of water between
    }

    @Test
    void eachShipCanOnlyBePlacedOnce() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.DESTROYER, 0, 0, Orientation.HORIZONTAL));
        assertFalse(board.placeShip(ShipType.DESTROYER, 0, 5, Orientation.HORIZONTAL));
        assertEquals(1, board.getShips().size());
    }

    @Test
    void attacksReportHitsMissesSinkingAndGameOver() {
        Board board = new Board();
        board.placeShip(ShipType.DESTROYER, 4, 4, Orientation.HORIZONTAL); // E5 and F5

        AttackOutcome miss = board.receiveAttack(0, 0);
        assertEquals(AttackResult.MISS, miss.getResult());
        assertNull(miss.getShip());

        AttackOutcome firstHit = board.receiveAttack(4, 4);
        assertEquals(AttackResult.HIT, firstHit.getResult());
        assertEquals(ShipType.DESTROYER, firstHit.getShipType());
        assertFalse(firstHit.isSunkShip());
        assertFalse(firstHit.isGameOver());

        AttackOutcome secondHit = board.receiveAttack(5, 4);
        assertTrue(secondHit.isSunkShip());
        assertTrue(secondHit.isGameOver());
        assertTrue(board.allShipsSunk());
    }

    @Test
    void attackingTheSameTileTwiceOrOffTheGridIsRejected() {
        Board board = new Board();
        board.receiveAttack(2, 2);
        assertEquals(AttackResult.ALREADY_ATTACKED, board.receiveAttack(2, 2).getResult());
        assertEquals(AttackResult.INVALID, board.receiveAttack(10, 0).getResult());
        assertEquals(AttackResult.INVALID, board.receiveAttack(0, -1).getResult());
    }

    @Test
    void randomizeAlwaysPlacesTheWholeFleet() {
        for (int seed = 0; seed < 200; seed++) {
            Board board = new Board();
            board.randomize(new Random(seed));
            assertTrue(board.allShipsPlaced(), "seed " + seed);
            assertEquals(ShipType.values().length, board.getShips().size(), "seed " + seed);
            for (ShipType type : ShipType.values()) {
                assertTrue(board.isShipPlaced(type), "seed " + seed + " is missing " + type);
            }
        }
    }

    @Test
    void clearRemovesShipsAndAttacks() {
        Board board = new Board();
        board.placeShip(ShipType.CRUISER, 0, 0, Orientation.VERTICAL);
        board.receiveAttack(0, 0);
        board.clear();
        assertTrue(board.getShips().isEmpty());
        assertFalse(board.getTile(0, 0).hasShip());
        assertFalse(board.getTile(0, 0).isAttacked());
    }
}
