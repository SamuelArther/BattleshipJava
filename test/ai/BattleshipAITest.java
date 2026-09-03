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
    void levelTwoShootsNextToAHit() {
        Board board = new Board();
        board.placeShip(ShipType.DESTROYER, 4, 4, Orientation.HORIZONTAL);
        BattleshipAI ai = new BattleshipAI(Difficulty.LEVEL_2, new Random(1));

        ai.handleShotResult(new Coordinate(4, 4), board.receiveAttack(4, 4));

        Coordinate next = ai.nextShot();
        int distance = Math.abs(next.x() - 4) + Math.abs(next.y() - 4);
        assertEquals(1, distance, "expected a neighbour of E5 but got " + next);
    }

    @Test
    void levelTwoStopsHuntingAroundASunkShip() {
        Board board = new Board();
        board.placeShip(ShipType.DESTROYER, 4, 4, Orientation.HORIZONTAL); // E5 and F5
        BattleshipAI ai = new BattleshipAI(Difficulty.LEVEL_2, new Random(1));

        ai.handleShotResult(new Coordinate(4, 4), board.receiveAttack(4, 4));
        AttackOutcome sunk = board.receiveAttack(5, 4);
        assertTrue(sunk.isSunkShip());
        ai.handleShotResult(new Coordinate(5, 4), sunk);

        for (int i = 0; i < 20; i++) {
            Coordinate shot = ai.nextShot();
            boolean nextToSunkShip = shot.x() >= 3 && shot.x() <= 6 && shot.y() >= 3 && shot.y() <= 5;
            assertFalse(nextToSunkShip, "shot " + shot + " is next to the sunk destroyer, which can't hold a ship");
        }
    }

    @Test
    void levelTwoWinsInFarFewerShotsThanLevelOne() {
        double levelOne = averageShotsToWin(Difficulty.LEVEL_1, 300);
        double levelTwo = averageShotsToWin(Difficulty.LEVEL_2, 300);
        assertTrue(levelOne > 90, "Level 1 averaged " + levelOne + " shots; random play needs most of the board");
        assertTrue(levelTwo < 65, "Level 2 averaged " + levelTwo + " shots; expected well under 65");
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
