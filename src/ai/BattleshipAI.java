package ai;

import game.AttackOutcome;
import game.AttackResult;
import game.Board;
import game.Coordinate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class BattleshipAI {
    private final Difficulty difficulty;
    private final Random random;
    /** Tiles the AI will never shoot: already attacked, or known to be empty. */
    private final boolean[][] ruledOut = new boolean[Board.SIZE][Board.SIZE];
    private final Queue<Coordinate> targetQueue = new ArrayDeque<>();

    public BattleshipAI(Difficulty difficulty) {
        this(difficulty, new Random());
    }

    public BattleshipAI(Difficulty difficulty, Random random) {
        this.difficulty = difficulty;
        this.random = random;
    }

    public Coordinate nextShot() {
        if (difficulty == Difficulty.LEVEL_2) {
            while (!targetQueue.isEmpty()) {
                Coordinate coordinate = targetQueue.poll();
                if (!ruledOut[coordinate.y()][coordinate.x()]) {
                    ruledOut[coordinate.y()][coordinate.x()] = true;
                    return coordinate;
                }
            }
        }

        List<Coordinate> available = new ArrayList<>();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (!ruledOut[y][x]) {
                    available.add(new Coordinate(x, y));
                }
            }
        }
        Collections.shuffle(available, random);
        Coordinate coordinate = available.getFirst();
        ruledOut[coordinate.y()][coordinate.x()] = true;
        return coordinate;
    }

    public void handleShotResult(Coordinate coordinate, AttackOutcome outcome) {
        if (difficulty != Difficulty.LEVEL_2 || outcome.getResult() != AttackResult.HIT) {
            return;
        }

        if (outcome.isSunkShip()) {
            // Ships can't touch (see Board.canPlaceShip), so every tile around a sunk ship is empty.
            targetQueue.clear();
            for (Coordinate shipTile : outcome.getShip().getCoordinates()) {
                ruleOutSurroundings(shipTile);
            }
            return;
        }

        addTarget(coordinate.x() + 1, coordinate.y());
        addTarget(coordinate.x() - 1, coordinate.y());
        addTarget(coordinate.x(), coordinate.y() + 1);
        addTarget(coordinate.x(), coordinate.y() - 1);
    }

    private void addTarget(int x, int y) {
        if (!isInBounds(x, y) || ruledOut[y][x]) {
            return;
        }
        targetQueue.offer(new Coordinate(x, y));
    }

    private void ruleOutSurroundings(Coordinate center) {
        for (int y = center.y() - 1; y <= center.y() + 1; y++) {
            for (int x = center.x() - 1; x <= center.x() + 1; x++) {
                if (isInBounds(x, y)) {
                    ruledOut[y][x] = true;
                }
            }
        }
    }

    private static boolean isInBounds(int x, int y) {
        return x >= 0 && x < Board.SIZE && y >= 0 && y < Board.SIZE;
    }
}
