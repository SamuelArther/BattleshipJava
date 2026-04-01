package ai;

import game.Board;
import game.Coordinate;
import game.ShipType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BattleshipAI {
    private final Difficulty difficulty;
    private final Random random;
    private final boolean[][] attempted = new boolean[Board.SIZE][Board.SIZE];

    // Confirmed hits not yet belonging to a sunk ship (Medium+)
    private final List<Coordinate> currentHits = new ArrayList<>();
    // Determined line direction for Hard/Nightmare: [dr, dc], or null
    private int[] direction = null;

    // US Navy: track hit/miss per cell and remaining ship sizes
    private static final int UNSHOT = 0, MISS = 1, HIT = 2;
    private final int[][] shotResult = new int[Board.SIZE][Board.SIZE];
    private final List<Integer> remainingShipSizes = new LinkedList<>();

    public BattleshipAI(Difficulty difficulty) {
        this(difficulty, new Random());
    }

    public BattleshipAI(Difficulty difficulty, Random random) {
        this.difficulty = difficulty;
        this.random = random;
        for (ShipType st : ShipType.values()) {
            remainingShipSizes.add(st.getSize());
        }
    }

    public Coordinate nextShot() {
        if (difficulty == Difficulty.EASY) {
            return markAttempted(randomUnshot());
        }

        if (difficulty == Difficulty.US_NAVY) {
            return markAttempted(probabilityShot());
        }

        // Medium / Hard / Nightmare: use targeting when we have hits
        if (!currentHits.isEmpty()) {
            Coordinate shot = getTargetShot();
            if (shot != null) return markAttempted(shot);
            currentHits.clear();
            direction = null;
        }

        return markAttempted(getHuntShot());
    }

    private Coordinate getHuntShot() {
        if (difficulty == Difficulty.NIGHTMARE) {
            List<Coordinate> parity = new ArrayList<>();
            for (int y = 0; y < Board.SIZE; y++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    if (!attempted[y][x] && (x + y) % 2 == 0) {
                        parity.add(new Coordinate(x, y));
                    }
                }
            }
            if (!parity.isEmpty()) {
                Collections.shuffle(parity, random);
                return parity.get(0);
            }
        }
        return randomUnshot();
    }

    private Coordinate getTargetShot() {
        if (difficulty == Difficulty.MEDIUM) {
            return anyAdjacentUnshot();
        }
        // Hard / Nightmare: directional
        if (direction != null) {
            Coordinate fwd = nextInDirection(direction[0], direction[1]);
            if (fwd != null) return fwd;
            Coordinate bwd = nextInDirection(-direction[0], -direction[1]);
            if (bwd != null) return bwd;
            direction = null;
        }
        return anyAdjacentUnshot();
    }

    // ── US Navy probability density map ────────────────────────────────────────

    private Coordinate probabilityShot() {
        int[][] prob = new int[Board.SIZE][Board.SIZE];

        for (int shipSize : remainingShipSizes) {
            // Horizontal placements
            for (int y = 0; y < Board.SIZE; y++) {
                for (int x = 0; x <= Board.SIZE - shipSize; x++) {
                    if (!placementValid(y, x, 0, 1, shipSize)) continue;
                    if (!currentHits.isEmpty() && !placementCoversHit(y, x, 0, 1, shipSize)) continue;
                    for (int i = 0; i < shipSize; i++) prob[y][x + i]++;
                }
            }
            // Vertical placements
            for (int y = 0; y <= Board.SIZE - shipSize; y++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    if (!placementValid(y, x, 1, 0, shipSize)) continue;
                    if (!currentHits.isEmpty() && !placementCoversHit(y, x, 1, 0, shipSize)) continue;
                    for (int i = 0; i < shipSize; i++) prob[y + i][x]++;
                }
            }
        }

        Coordinate best = null;
        int bestScore = -1;
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (!attempted[y][x] && prob[y][x] > bestScore) {
                    bestScore = prob[y][x];
                    best = new Coordinate(x, y);
                }
            }
        }
        return best != null ? best : randomUnshot();
    }

    // Returns true if a placement starting at (y,x) in direction (dy,dx) contains no 'miss' cells
    private boolean placementValid(int y, int x, int dy, int dx, int size) {
        for (int i = 0; i < size; i++) {
            if (shotResult[y + i * dy][x + i * dx] == MISS) return false;
        }
        return true;
    }

    // Returns true if the placement covers at least one cell in currentHits
    private boolean placementCoversHit(int y, int x, int dy, int dx, int size) {
        for (Coordinate h : currentHits) {
            for (int i = 0; i < size; i++) {
                if (h.y() == y + i * dy && h.x() == x + i * dx) return true;
            }
        }
        return false;
    }

    // ── Direction helpers ───────────────────────────────────────────────────────

    private Coordinate nextInDirection(int dr, int dc) {
        Coordinate extreme = extremeHit(dr, dc);
        if (extreme == null) return null;
        int nr = extreme.y() + dr;
        int nc = extreme.x() + dc;
        if (nr < 0 || nr >= Board.SIZE || nc < 0 || nc >= Board.SIZE) return null;
        if (attempted[nr][nc]) return null;
        return new Coordinate(nc, nr);
    }

    private Coordinate extremeHit(int dr, int dc) {
        Coordinate best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Coordinate h : currentHits) {
            int score = h.y() * dr + h.x() * dc;
            if (score > bestScore) { bestScore = score; best = h; }
        }
        return best;
    }

    private Coordinate anyAdjacentUnshot() {
        Set<String> seen = new HashSet<>();
        List<Coordinate> candidates = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (Coordinate h : currentHits) {
            for (int[] d : dirs) {
                int nr = h.y() + d[0], nc = h.x() + d[1];
                String key = nr + "," + nc;
                if (nr >= 0 && nr < Board.SIZE && nc >= 0 && nc < Board.SIZE
                        && !attempted[nr][nc] && seen.add(key)) {
                    candidates.add(new Coordinate(nc, nr));
                }
            }
        }
        if (candidates.isEmpty()) return null;
        Collections.shuffle(candidates, random);
        return candidates.get(0);
    }

    private Coordinate randomUnshot() {
        List<Coordinate> available = new ArrayList<>();
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (!attempted[y][x]) available.add(new Coordinate(x, y));
            }
        }
        Collections.shuffle(available, random);
        return available.get(0);
    }

    private Coordinate markAttempted(Coordinate c) {
        attempted[c.y()][c.x()] = true;
        return c;
    }

    // ── Public callbacks ────────────────────────────────────────────────────────

    public void handleShotResult(Coordinate coordinate, boolean hit) {
        shotResult[coordinate.y()][coordinate.x()] = hit ? HIT : MISS;
        if (difficulty == Difficulty.EASY || !hit) return;

        currentHits.add(coordinate);

        if ((difficulty == Difficulty.HARD || difficulty == Difficulty.NIGHTMARE
                || difficulty == Difficulty.US_NAVY) && currentHits.size() >= 2) {
            Coordinate first = currentHits.get(0);
            Coordinate last  = currentHits.get(currentHits.size() - 1);
            int dr = Integer.signum(last.y() - first.y());
            int dc = Integer.signum(last.x() - first.x());
            if (dr != 0 || dc != 0) direction = new int[]{dr, dc};
        }
    }

    public void markUnavailable(Collection<Coordinate> coordinates) {
        for (Coordinate c : coordinates) {
            attempted[c.y()][c.x()] = true;
        }
        // Remove from remaining ship sizes (match by count of coordinates)
        remainingShipSizes.remove(Integer.valueOf(coordinates.size()));

        Set<Coordinate> sunk = new HashSet<>(coordinates);
        currentHits.removeIf(sunk::contains);
        if (currentHits.size() < 2) direction = null;
    }
}
