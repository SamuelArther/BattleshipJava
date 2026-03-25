package network;

public interface NetworkMessageListener {
    default void onWaitingForOpponent(String message) {
    }

    default void onConnected() {
    }

    default void onPlaceShips() {
    }

    default void onRemoteReady() {
    }

    default void onGameStart(boolean myTurn) {
    }

    default void onTurnGranted() {
    }

    default void onAttackReceived(int x, int y) {
    }

    default void onAttackResult(boolean hit) {
    }

    default void onWin() {
    }

    default void onLose() {
    }

    default void onDisconnected(String message) {
    }

    default void onError(String message) {
    }
}
