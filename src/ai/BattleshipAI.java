package ai;

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
    private final boolean[][] attemptedShots = new boolean[Board.SIZE][Board.SIZE];
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
                if (!attemptedShots[coordinate.y()][coordinate.x()]) {
                    attemptedShots[coordinate.y()][coordinate.x()] = true;
                    return coordinate;
                }
            }
        }

        List<Coordinate> available = new ArrayList<>();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (!attemptedShots[y][x]) {
                    available.add(new Coordinate(x, y));
                }
            }
        }
        Collections.shuffle(available, random);
        Coordinate coordinate = available.getFirst();
        attemptedShots[coordinate.y()][coordinate.x()] = true;
        return coordinate;
    }

    public void handleShotResult(Coordinate coordinate, boolean hit) {
        if (difficulty != Difficulty.LEVEL_2 || !hit) {
            return;
        }

        addTarget(coordinate.x() + 1, coordinate.y());
        addTarget(coordinate.x() - 1, coordinate.y());
        addTarget(coordinate.x(), coordinate.y() + 1);
        addTarget(coordinate.x(), coordinate.y() - 1);
    }

    private void addTarget(int x, int y) {
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE || attemptedShots[y][x]) {
            return;
        }
        targetQueue.offer(new Coordinate(x, y));
    }
}
