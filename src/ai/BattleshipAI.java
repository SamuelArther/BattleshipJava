package ai;

import game.AttackOutcome;
import game.AttackResult;
import game.Board;
import game.Coordinate;
import game.ShipType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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
    private static final int UNSHOT = 0, MISS = 1, HIT = 2, SUNK = 3;
    private static final int[][] STEPS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
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

        if (difficulty == Difficulty.US_NAVY || difficulty == Difficulty.ARMED_FORCES) {
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

    // ── Probability density map (US Navy and All of the US Armed Forces) ────────

    /**
     * Counts, for every square, how many ways the ships still afloat could be lying across it,
     * and fires at the square that shows up in the most of them.
     *
     * All of the US Armed Forces adds two things on top of that count, neither of which needs
     * any knowledge of where the ships actually are. Placements lining up with squares already
     * hit are weighted far above the rest, so a confirmed hit is pursued to the end instead of
     * merely nudging the odds. And with no hit outstanding it only sweeps squares on the
     * tightest diagonal lattice that the shortest surviving ship cannot fit through, which
     * throws away the squares that could not possibly finish the hunt any sooner.
     */
    private Coordinate probabilityShot() {
        boolean relentless = difficulty == Difficulty.ARMED_FORCES;
        long[][] prob = new long[Board.SIZE][Board.SIZE];

        for (int shipSize : remainingShipSizes) {
            for (int y = 0; y < Board.SIZE; y++) {
                for (int x = 0; x <= Board.SIZE - shipSize; x++) {
                    score(prob, y, x, 0, 1, shipSize, relentless);
                }
            }
            for (int y = 0; y <= Board.SIZE - shipSize; y++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    score(prob, y, x, 1, 0, shipSize, relentless);
                }
            }
        }

        boolean hunting = currentHits.isEmpty();
        int stride = relentless && hunting ? shortestRemainingShip() : 1;

        List<Coordinate> best = pick(prob, stride);
        if (best.isEmpty() && stride > 1) {
            best = pick(prob, 1); // The lattice can run out before the board does.
        }
        return best.isEmpty() ? randomUnshot() : best.get(random.nextInt(best.size()));
    }

    /** Adds one placement's contribution to the map, if that placement is still possible. */
    private void score(long[][] prob, int y, int x, int dy, int dx, int size, boolean weighted) {
        if (!placementValid(y, x, dy, dx, size)) return;
        int covered = hitsCovered(y, x, dy, dx, size);
        if (!currentHits.isEmpty() && covered == 0) return;

        // Four times the weight per confirmed hit the placement explains.
        long weight = weighted ? 1L << (2 * covered) : 1L;
        for (int i = 0; i < size; i++) {
            int cy = y + i * dy;
            int cx = x + i * dx;
            if (!attempted[cy][cx]) {
                prob[cy][cx] += weight;
            }
        }
    }

    private List<Coordinate> pick(long[][] prob, int stride) {
        List<Coordinate> best = new ArrayList<>();
        long bestScore = -1;
        for (int y = 0; y < Board.SIZE; y++) {
            for (int x = 0; x < Board.SIZE; x++) {
                if (attempted[y][x] || prob[y][x] == 0) continue;
                if (stride > 1 && (x + y) % stride != 0) continue;
                if (prob[y][x] < bestScore) continue;
                if (prob[y][x] > bestScore) {
                    bestScore = prob[y][x];
                    best.clear();
                }
                best.add(new Coordinate(x, y));
            }
        }
        return best;
    }

    private int shortestRemainingShip() {
        int shortest = Board.SIZE;
        for (int size : remainingShipSizes) {
            shortest = Math.min(shortest, size);
        }
        return Math.max(1, shortest);
    }

    private int hitsCovered(int y, int x, int dy, int dx, int size) {
        int covered = 0;
        for (Coordinate hit : currentHits) {
            for (int i = 0; i < size; i++) {
                if (hit.y() == y + i * dy && hit.x() == x + i * dx) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }

    // Returns true if a placement starting at (y,x) in direction (dy,dx) contains no 'miss' cells
    private boolean placementValid(int y, int x, int dy, int dx, int size) {
        for (int i = 0; i < size; i++) {
            int state = shotResult[y + i * dy][x + i * dx];
            if (state == MISS || state == SUNK) return false;
        }
        return true;
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

    public void handleShotResult(Coordinate coordinate, AttackOutcome outcome) {
        boolean hit = outcome.getResult() == AttackResult.HIT;
        shotResult[coordinate.y()][coordinate.x()] = hit ? HIT : MISS;
        if (!hit) return;

        currentHits.add(coordinate);

        // Every difficulty learns where the wrecks are; only the hunt for the next one differs.
        if (outcome.isSunkShip()) {
            retireSunkShip(coordinate, outcome);
            return;
        }
        if (difficulty == Difficulty.EASY) return;

        if ((difficulty == Difficulty.HARD || difficulty == Difficulty.NIGHTMARE
                || difficulty == Difficulty.US_NAVY || difficulty == Difficulty.ARMED_FORCES) && currentHits.size() >= 2) {
            Coordinate first = currentHits.get(0);
            Coordinate last  = currentHits.get(currentHits.size() - 1);
            int dr = Integer.signum(last.y() - first.y());
            int dc = Integer.signum(last.x() - first.x());
            if (dr != 0 || dc != 0) direction = new int[]{dr, dc};
        }
    }

    /**
     * A ship has gone down. Its own squares can never hold another ship, and because ships are
     * not allowed to touch, neither can the water the board cleared around it. Forgetting either
     * leaves the probability map piling shots onto wrecks it has already sunk.
     */
    private void retireSunkShip(Coordinate lastHit, AttackOutcome outcome) {
        Set<Coordinate> shipTiles = connectedHits(lastHit);
        for (Coordinate tile : shipTiles) {
            shotResult[tile.y()][tile.x()] = SUNK;
            attempted[tile.y()][tile.x()] = true;
        }
        for (Coordinate water : outcome.getClearedCoordinates()) {
            shotResult[water.y()][water.x()] = MISS;
            attempted[water.y()][water.x()] = true;
        }
        currentHits.removeAll(shipTiles);
        if (outcome.getShipType() != null) {
            remainingShipSizes.remove(Integer.valueOf(outcome.getShipType().getSize()));
        }
        if (currentHits.size() < 2) direction = null;
    }

    /** The hits joined to {@code start} edge to edge, which is exactly one ship since ships never touch. */
    private Set<Coordinate> connectedHits(Coordinate start) {
        Set<Coordinate> hits = new HashSet<>(currentHits);
        Set<Coordinate> found = new HashSet<>();
        Deque<Coordinate> queue = new ArrayDeque<>();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Coordinate c = queue.poll();
            for (int[] step : STEPS) {
                Coordinate neighbour = new Coordinate(c.x() + step[1], c.y() + step[0]);
                if (hits.contains(neighbour) && found.add(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return found;
    }
}
