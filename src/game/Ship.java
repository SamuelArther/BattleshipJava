package game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ship {
    private final ShipType type;
    private final List<Coordinate> coordinates = new ArrayList<>();
    private final Set<Coordinate> hits = new HashSet<>();

    public Ship(ShipType type) {
        this.type = type;
    }

    public ShipType getType() {
        return type;
    }

    public List<Coordinate> getCoordinates() {
        return List.copyOf(coordinates);
    }

    public void addCoordinate(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    public void registerHit(Coordinate coordinate) {
        hits.add(coordinate);
    }

    public boolean isSunk() {
        return hits.size() == coordinates.size();
    }
}
