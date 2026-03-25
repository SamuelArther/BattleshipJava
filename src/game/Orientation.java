package game;

public enum Orientation {
    HORIZONTAL,
    VERTICAL;

    public Orientation toggle() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }
}
