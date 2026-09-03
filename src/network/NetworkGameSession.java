package network;

import game.AttackOutcome;
import game.AttackResult;
import game.ShipType;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkGameSession {
    public static final int DEFAULT_PORT = 50505;

    private final Object sendLock = new Object();
    private volatile NetworkMessageListener listener;
    private volatile ServerSocket serverSocket;
    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;
    private volatile boolean host;
    private volatile boolean localReady;
    private volatile boolean remoteReady;
    private volatile boolean started;
    private volatile boolean closed;

    public NetworkGameSession(NetworkMessageListener listener) {
        this.listener = listener;
    }

    public void setListener(NetworkMessageListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean isHost() {
        return host;
    }

    public void host(int port) {
        host = true;
        Thread hostThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                dispatchWaiting("Hosting on port " + port + ". Waiting for a player...");
                Socket accepted = serverSocket.accept();
                initializeSocket(accepted);
                sendMessage("PLACE_SHIPS");
                dispatch(NetworkMessageListener::onConnected);
                dispatch(NetworkMessageListener::onPlaceShips);
            } catch (IOException exception) {
                if (!closed) {
                    dispatchError("Unable to host game: " + exception.getMessage());
                }
            }
        }, "battleship-host-thread");
        hostThread.setDaemon(true);
        hostThread.start();
    }

    public void join(String hostAddress, int port) {
        host = false;
        Thread joinThread = new Thread(() -> {
            try {
                dispatchWaiting("Connecting to " + hostAddress + ":" + port + "...");
                Socket connected = new Socket();
                connected.connect(new InetSocketAddress(hostAddress, port), 4000);
                initializeSocket(connected);
                dispatch(NetworkMessageListener::onConnected);
            } catch (IOException exception) {
                if (!closed) {
                    dispatchError("Unable to connect: " + exception.getMessage());
                }
            }
        }, "battleship-join-thread");
        joinThread.setDaemon(true);
        joinThread.start();
    }

    public void sendReady() {
        localReady = true;
        sendMessage("READY");
        attemptGameStart();
    }

    public void sendAttack(int x, int y) {
        sendMessage("ATTACK:" + x + "," + y);
    }

    public void sendResult(AttackOutcome outcome) {
        if (outcome.getResult() != AttackResult.HIT) {
            sendMessage("RESULT:MISS");
        } else if (outcome.isSunkShip()) {
            sendMessage("RESULT:HIT:SUNK:" + outcome.getShipType().name());
        } else {
            sendMessage("RESULT:HIT");
        }
    }

    public void sendTurn() {
        sendMessage("TURN");
    }

    public void sendWin() {
        sendMessage("WIN");
    }

    public void sendLose() {
        sendMessage("LOSE");
    }

    public void close() {
        closed = true;
        tryClose(reader);
        tryClose(writer);
        tryClose(socket);
        tryClose(serverSocket);
    }

    private void initializeSocket(Socket connectedSocket) throws IOException {
        socket = connectedSocket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        Thread readerThread = new Thread(this::readLoop, "battleship-network-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try {
            String line;
            while (!closed && (line = reader.readLine()) != null) {
                handleMessage(line.trim());
            }
            if (!closed) {
                dispatchDisconnected("The other player disconnected.");
            }
        } catch (IOException exception) {
            if (!closed) {
                dispatchDisconnected("Network connection lost.");
            }
        } finally {
            close();
        }
    }

    private void handleMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        if ("PLACE_SHIPS".equals(message)) {
            dispatch(NetworkMessageListener::onPlaceShips);
            return;
        }
        if ("READY".equals(message)) {
            remoteReady = true;
            dispatch(NetworkMessageListener::onRemoteReady);
            attemptGameStart();
            return;
        }
        if ("TURN".equals(message)) {
            if (!started) {
                started = true;
                dispatch(listener -> listener.onGameStart(true));
            } else {
                dispatch(NetworkMessageListener::onTurnGranted);
            }
            return;
        }
        if (message.startsWith("RESULT:")) {
            String[] parts = message.split(":");
            boolean hit = parts.length >= 2 && "HIT".equals(parts[1]);
            ShipType sunkShip = null;
            if (hit && parts.length >= 4 && "SUNK".equals(parts[2])) {
                try {
                    sunkShip = ShipType.valueOf(parts[3]);
                } catch (IllegalArgumentException exception) {
                    dispatchError("Received an invalid result message.");
                    return;
                }
            }
            ShipType sunk = sunkShip;
            dispatch(listener -> listener.onAttackResult(hit, sunk));
            return;
        }
        if ("WIN".equals(message)) {
            dispatch(NetworkMessageListener::onLose);
            return;
        }
        if ("LOSE".equals(message)) {
            dispatch(NetworkMessageListener::onWin);
            return;
        }
        if (message.startsWith("ATTACK:")) {
            String payload = message.substring("ATTACK:".length());
            String[] coordinateParts = payload.split(",");
            if (coordinateParts.length == 2) {
                try {
                    int x = Integer.parseInt(coordinateParts[0]);
                    int y = Integer.parseInt(coordinateParts[1]);
                    dispatch(listener -> listener.onAttackReceived(x, y));
                } catch (NumberFormatException exception) {
                    dispatchError("Received an invalid attack message.");
                }
            }
        }
    }

    private void attemptGameStart() {
        if (host && localReady && remoteReady && !started) {
            started = true;
            dispatch(listener -> listener.onGameStart(false));
            sendTurn();
        }
    }

    private void sendMessage(String message) {
        synchronized (sendLock) {
            if (writer == null || closed) {
                return;
            }
            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException exception) {
                dispatchDisconnected("Unable to send network message.");
                close();
            }
        }
    }

    private void dispatchWaiting(String message) {
        dispatch(listener -> listener.onWaitingForOpponent(message));
    }

    private void dispatchDisconnected(String message) {
        dispatch(listener -> listener.onDisconnected(message));
    }

    private void dispatchError(String message) {
        dispatch(listener -> listener.onError(message));
    }

    private void dispatch(java.util.function.Consumer<NetworkMessageListener> callback) {
        NetworkMessageListener currentListener = listener;
        if (currentListener == null) {
            return;
        }
        Platform.runLater(() -> callback.accept(currentListener));
    }

    private void tryClose(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
