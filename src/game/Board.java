package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class Board {
    public static final int SIZE = 10;

    private final Tile[][] tiles = new Tile[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();

    public Board() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                tiles[y][x] = new Tile(x, y);
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) {
            throw new IllegalArgumentException("Coordinate out of bounds: " + x + "," + y);
        }
        return tiles[y][x];
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public List<Ship> getShips() {
        return List.copyOf(ships);
    }

    public boolean canPlaceShip(ShipType type, int startX, int startY, Orientation orientation) {
        for (int i = 0; i < type.getSize(); i++) {
            int x = startX + (orientation == Orientation.HORIZONTAL ? i : 0);
            int y = startY + (orientation == Orientation.VERTICAL ? i : 0);
            if (!isInBounds(x, y) || getTile(x, y).hasShip() || touchesAnotherShip(x, y)) {
                return false;
            }
        }
        return true;
    }

    public boolean placeShip(ShipType type, int startX, int startY, Orientation orientation) {
        if (isShipPlaced(type) || !canPlaceShip(type, startX, startY, orientation)) {
            return false;
        }

        Ship ship = new Ship(type);
        for (int i = 0; i < type.getSize(); i++) {
            int x = startX + (orientation == Orientation.HORIZONTAL ? i : 0);
            int y = startY + (orientation == Orientation.VERTICAL ? i : 0);
            Coordinate coordinate = new Coordinate(x, y);
            ship.addCoordinate(coordinate);
            getTile(x, y).setShip(ship);
        }
        ships.add(ship);
        return true;
    }

    public void clear() {
        ships.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Tile tile = tiles[y][x];
                tile.setShip(null);
                tile.setAttacked(false);
            }
        }
    }

    public void randomize(Random random) {
        clear();
        List<ShipType> types = new ArrayList<>(EnumSet.allOf(ShipType.class));
        Collections.shuffle(types, random);
        for (ShipType type : types) {
            boolean placed = false;
            while (!placed) {
                int x = random.nextInt(SIZE);
                int y = random.nextInt(SIZE);
                Orientation orientation = random.nextBoolean() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                placed = placeShip(type, x, y, orientation);
            }
        }
    }

    public boolean isShipPlaced(ShipType type) {
        return ships.stream().anyMatch(ship -> ship.getType() == type);
    }

    public boolean allShipsPlaced() {
        return ships.size() == ShipType.values().length;
    }

    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    public AttackOutcome receiveAttack(int x, int y) {
        if (!isInBounds(x, y)) {
            return new AttackOutcome(AttackResult.INVALID, false, false, null);
        }

        Tile tile = getTile(x, y);
        if (tile.isAttacked()) {
            return new AttackOutcome(AttackResult.ALREADY_ATTACKED, false, false, tile.hasShip() ? tile.getShip().getType() : null);
        }

        tile.setAttacked(true);
        if (!tile.hasShip()) {
            return new AttackOutcome(AttackResult.MISS, false, false, null);
        }

        Ship ship = tile.getShip();
        ship.registerHit(new Coordinate(x, y));
        boolean sunk = ship.isSunk();
        boolean gameOver = allShipsSunk();
        return new AttackOutcome(AttackResult.HIT, sunk, gameOver, ship.getType());
    }

    private boolean touchesAnotherShip(int x, int y) {
        for (int checkY = y - 1; checkY <= y + 1; checkY++) {
            for (int checkX = x - 1; checkX <= x + 1; checkX++) {
                if (isInBounds(checkX, checkY) && getTile(checkX, checkY).hasShip()) {
                    return true;
                }
            }
        }
        return false;
    }
}
