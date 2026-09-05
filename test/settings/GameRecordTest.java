package settings;

import game.Board;
import game.Orientation;
import game.ShipType;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fleet-shape achievements are read off the finished board, so the reading has to be right.
 * These place real fleets that obey the no-touching rule and check what the record says about them.
 */
class GameRecordTest {

    private static GameRecord describe(Board board) {
        GameRecord record = new GameRecord();
        record.describeFleet(board);
        return record;
    }

    /** Five ships along alternating rows, all lying flat, none of them near an edge. */
    private static Board inlandHorizontalFleet() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 2, 1, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.BATTLESHIP, 2, 3, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.CRUISER, 2, 5, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.SUBMARINE, 2, 7, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.DESTROYER, 6, 7, Orientation.HORIZONTAL));
        return board;
    }

    @Test
    void readsOrientationOffTheBoard() {
        GameRecord flat = describe(inlandHorizontalFleet());
        assertTrue(flat.allHorizontal);
        assertFalse(flat.allVertical);

        Board upright = new Board();
        assertTrue(upright.placeShip(ShipType.CARRIER, 1, 2, Orientation.VERTICAL));
        assertTrue(upright.placeShip(ShipType.BATTLESHIP, 3, 2, Orientation.VERTICAL));
        assertTrue(upright.placeShip(ShipType.CRUISER, 5, 2, Orientation.VERTICAL));
        assertTrue(upright.placeShip(ShipType.SUBMARINE, 7, 2, Orientation.VERTICAL));
        assertTrue(upright.placeShip(ShipType.DESTROYER, 5, 6, Orientation.VERTICAL));
        GameRecord standing = describe(upright);
        assertTrue(standing.allVertical);
        assertFalse(standing.allHorizontal);
    }

    @Test
    void noticesAFleetKeptOffTheEdges() {
        GameRecord record = describe(inlandHorizontalFleet());
        assertTrue(record.noShipOnEdge);
        assertFalse(record.everyShipOnEdge);
    }

    @Test
    void noticesAFleetPushedOntoTheEdges() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));      // A1 to E1
        assertTrue(board.placeShip(ShipType.BATTLESHIP, 0, 9, Orientation.HORIZONTAL));   // A10 to D10
        assertTrue(board.placeShip(ShipType.CRUISER, 9, 0, Orientation.VERTICAL));        // J1 to J3
        assertTrue(board.placeShip(ShipType.SUBMARINE, 9, 7, Orientation.VERTICAL));      // J8 to J10
        assertTrue(board.placeShip(ShipType.DESTROYER, 0, 4, Orientation.VERTICAL));      // A5 to A6
        GameRecord record = describe(board);
        assertTrue(record.everyShipOnEdge);
        assertFalse(record.noShipOnEdge);
        assertTrue(record.shipInEveryCorner);
        assertTrue(record.shipInEveryQuarter);
        assertFalse(record.withinOneHalf);
    }

    @Test
    void noticesAFleetCrowdedIntoOneHalf() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.BATTLESHIP, 0, 2, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.CRUISER, 0, 4, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL));
        assertTrue(board.placeShip(ShipType.DESTROYER, 6, 2, Orientation.HORIZONTAL));
        GameRecord record = describe(board);
        assertTrue(record.withinOneHalf);
        assertFalse(record.shipInEveryQuarter);
    }

    @Test
    void anIncompleteFleetIsNotDescribed() {
        Board board = new Board();
        assertTrue(board.placeShip(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL));
        GameRecord record = describe(board);
        // Left at their defaults rather than claiming something about four ships that are not there.
        assertFalse(record.everyShipOnEdge);
        assertFalse(record.allHorizontal);
    }

    @Test
    void remembersWhetherTheBoardWasRandomized() {
        Board board = new Board();
        board.randomize(new Random(7));
        assertTrue(describe(board).randomizedBoard);

        assertTrue(describe(inlandHorizontalFleet()).randomizedBoard == false);
    }

    @Test
    void readsTheOrderTheEnemyFleetWentDown() {
        GameRecord largestFirst = new GameRecord();
        largestFirst.sinkOrder.add(ShipType.CARRIER);
        largestFirst.sinkOrder.add(ShipType.BATTLESHIP);
        largestFirst.sinkOrder.add(ShipType.CRUISER);
        largestFirst.sinkOrder.add(ShipType.SUBMARINE);
        largestFirst.sinkOrder.add(ShipType.DESTROYER);
        assertTrue(largestFirst.sunkLargestFirst());
        assertFalse(largestFirst.sunkSmallestFirst());

        GameRecord smallestFirst = new GameRecord();
        smallestFirst.sinkOrder.add(ShipType.DESTROYER);
        smallestFirst.sinkOrder.add(ShipType.SUBMARINE);
        smallestFirst.sinkOrder.add(ShipType.CRUISER);
        smallestFirst.sinkOrder.add(ShipType.BATTLESHIP);
        smallestFirst.sinkOrder.add(ShipType.CARRIER);
        assertTrue(smallestFirst.sunkSmallestFirst());
        assertFalse(smallestFirst.sunkLargestFirst());

        GameRecord jumbled = new GameRecord();
        jumbled.sinkOrder.add(ShipType.CRUISER);
        jumbled.sinkOrder.add(ShipType.CARRIER);
        jumbled.sinkOrder.add(ShipType.DESTROYER);
        jumbled.sinkOrder.add(ShipType.BATTLESHIP);
        jumbled.sinkOrder.add(ShipType.SUBMARINE);
        assertFalse(jumbled.sunkLargestFirst());
        assertFalse(jumbled.sunkSmallestFirst());

        // An unfinished fleet is not an order at all.
        GameRecord partial = new GameRecord();
        partial.sinkOrder.add(ShipType.CARRIER);
        assertFalse(partial.sunkLargestFirst());
        assertFalse(partial.sunkSmallestFirst());
    }

    @Test
    void everyAchievementIsSpokenForByACategory() {
        for (Achievements.Achievement achievement : Achievements.Achievement.values()) {
            assertTrue(achievement.getCategory() != null, achievement.name() + " has no category");
            assertFalse(achievement.getDisplayName().isBlank(), achievement.name() + " has no name");
            assertFalse(achievement.getDescription().isBlank(), achievement.name() + " has no description");
        }
        int counted = 0;
        for (Achievements.Category category : Achievements.Category.values()) {
            counted += Achievements.totalIn(category);
        }
        assertTrue(counted == Achievements.Achievement.values().length);
        assertTrue(Achievements.Achievement.values().length == 125);
    }
}
