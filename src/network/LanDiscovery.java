package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.function.Consumer;

public class LanDiscovery {
    public static final int DISCOVERY_PORT = 50506;
    // Omit look-alike characters: 0/O, 1/I
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int CODE_LENGTH = 6;
    private static final String PREFIX = "BS:";
    private static final int BROADCAST_INTERVAL_MS = 600;
    private static final int LISTEN_TIMEOUT_MS = 15000;

    private volatile boolean broadcasting;
    private volatile Thread broadcastThread;

    public static String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    public void startBroadcasting(String code) {
        broadcasting = true;
        broadcastThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] data = (PREFIX + code).getBytes(StandardCharsets.UTF_8);
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);
                while (broadcasting) {
                    try {
                        socket.send(packet);
                        Thread.sleep(BROADCAST_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }, "battleship-broadcast");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    public void stopBroadcasting() {
        broadcasting = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    public static void findHost(String code, Consumer<String> onFound, Runnable onTimeout) {
        String target = PREFIX + code.trim().toUpperCase();
        Thread listenThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                socket.setBroadcast(true);
                socket.setSoTimeout(500);
                byte[] buffer = new byte[256];
                long deadline = System.currentTimeMillis() + LISTEN_TIMEOUT_MS;
                while (System.currentTimeMillis() < deadline) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                        if (target.equals(msg)) {
                            onFound.accept(packet.getAddress().getHostAddress());
                            return;
                        }
                    } catch (IOException ignored) {
                        // receive timed out, loop again
                    }
                }
                onTimeout.run();
            } catch (IOException e) {
                onTimeout.run();
            }
        }, "battleship-discovery");
        listenThread.setDaemon(true);
        listenThread.start();
    }
}
