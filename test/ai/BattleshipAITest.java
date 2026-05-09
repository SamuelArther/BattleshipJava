package ai;

import game.AttackOutcome;
import game.AttackResult;
import game.Board;
import game.Coordinate;
import game.Orientation;
import game.ShipType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleshipAITest {

    @Test
    void neverShootsTheSameTileTwice() {
        for (Difficulty difficulty : Difficulty.values()) {
            BattleshipAI ai = new BattleshipAI(difficulty, new Random(1));
            Set<Coordinate> shots = new HashSet<>();
            for (int i = 0; i < Board.SIZE * Board.SIZE; i++) {
                assertTrue(shots.add(ai.nextShot()), difficulty + " repeated a shot");
            }
        }
    }

    @Test
    void mediumShootsNextToAHit() {
        Board board = new Board();
        board.placeShip(ShipType.DESTROYER, 4, 4, Orientation.HORIZONTAL);
        BattleshipAI ai = new BattleshipAI(Difficulty.MEDIUM, new Random(1));

        ai.handleShotResult(new Coordinate(4, 4), board.receiveAttack(4, 4));

        Coordinate next = ai.nextShot();
        int distance = Math.abs(next.x() - 4) + Math.abs(next.y() - 4);
        assertEquals(1, distance, "expected a neighbour of E5 but got " + next);
    }

    @Test
    void stopsHuntingTheWaterAroundASunkShip() {
        // Ships cannot touch each other, so once one sinks every square around it is known to be empty.
        for (Difficulty difficulty : Difficulty.values()) {
            Board board = new Board();
            board.placeShip(ShipType.DESTROYER, 4, 4, Orientation.HORIZONTAL); // E5 and F5
            BattleshipAI ai = new BattleshipAI(difficulty, new Random(1));

            ai.handleShotResult(new Coordinate(4, 4), board.receiveAttack(4, 4));
            AttackOutcome sunk = board.receiveAttack(5, 4);
            assertTrue(sunk.isSunkShip());
            ai.handleShotResult(new Coordinate(5, 4), sunk);

            Set<Coordinate> knownEmpty = new HashSet<>(sunk.getClearedCoordinates());
            knownEmpty.add(new Coordinate(4, 4));
            knownEmpty.add(new Coordinate(5, 4));
            assertFalse(knownEmpty.isEmpty(), "expected the water around the destroyer to be cleared");
            for (int i = 0; i < 20; i++) {
                Coordinate shot = ai.nextShot();
                assertFalse(knownEmpty.contains(shot),
                    difficulty + " shot " + shot + ", which is the sunk destroyer or the cleared water around it");
            }
        }
    }

    @Test
    void harderDifficultiesNeedFewerShots() {
        double easy = averageShotsToWin(Difficulty.EASY, 200);
        double medium = averageShotsToWin(Difficulty.MEDIUM, 200);
        double hard = averageShotsToWin(Difficulty.HARD, 200);
        double nightmare = averageShotsToWin(Difficulty.NIGHTMARE, 200);
        double usNavy = averageShotsToWin(Difficulty.US_NAVY, 200);

        assertTrue(easy > 80, "Easy averaged " + easy + " shots; random play needs most of the board");
        assertTrue(medium < easy, "Medium (" + medium + ") should beat Easy (" + easy + ")");
        assertTrue(hard < medium, "Hard (" + hard + ") should beat Medium (" + medium + ")");
        assertTrue(nightmare < hard, "Nightmare (" + nightmare + ") should beat Hard (" + hard + ")");
        assertTrue(usNavy < nightmare, "US Navy (" + usNavy + ") should beat Nightmare (" + nightmare + ")");
        double armedForces = averageShotsToWin(Difficulty.ARMED_FORCES, 200);
        assertTrue(armedForces < usNavy,
            "All of the US Armed Forces (" + armedForces + ") should beat US Navy (" + usNavy + ")");
        assertTrue(armedForces < 40,
            "All of the US Armed Forces averaged " + armedForces + " shots; expected under 40");
    }

    /** Plays complete games the same way GameScene drives the AI, and returns the average shots per win. */
    private static double averageShotsToWin(Difficulty difficulty, int games) {
        Random seeds = new Random(42);
        long totalShots = 0;
        for (int game = 0; game < games; game++) {
            Board board = new Board();
            board.randomize(new Random(seeds.nextLong()));
            BattleshipAI ai = new BattleshipAI(difficulty, new Random(seeds.nextLong()));
            int shots = 0;
            AttackOutcome outcome;
            do {
                Coordinate shot = ai.nextShot();
                outcome = board.receiveAttack(shot.x(), shot.y());
                assertNotEquals(AttackResult.ALREADY_ATTACKED, outcome.getResult(), "repeated shot at " + shot);
                assertNotEquals(AttackResult.INVALID, outcome.getResult(), "off-grid shot at " + shot);
                ai.handleShotResult(shot, outcome);
                shots++;
            } while (!outcome.isGameOver());
            totalShots += shots;
        }
        return totalShots / (double) games;
    }
}
