package dev.jabe.client.network;

import dev.jabe.client.JabeClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Discovers Bedrock worlds using RakNet unconnected ping/pong packets. */
public final class BedrockLanDiscovery implements AutoCloseable {
    public static final int DEFAULT_PORT = 19132;
    private static final byte UNCONNECTED_PING = 0x01;
    private static final byte UNCONNECTED_PONG = 0x1c;
    private static final byte[] OFFLINE_MESSAGE_ID = {
            0x00, (byte) 0xff, (byte) 0xff, 0x00,
            (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe,
            (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
            0x12, 0x34, 0x56, 0x78
    };
    private static final BedrockLanDiscovery INSTANCE = new BedrockLanDiscovery();

    private final AtomicReference<List<BedrockLanWorld>> worlds = new AtomicReference<>(List.of());
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService executor;

    private BedrockLanDiscovery() {
    }

    public static BedrockLanDiscovery getInstance() {
        return INSTANCE;
    }

    public List<BedrockLanWorld> snapshot() {
        return worlds.get();
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jabe-bedrock-lan-discovery");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::scanSafely, 0, 2, TimeUnit.SECONDS);
    }

    public void refreshNow() {
        ScheduledExecutorService activeExecutor = executor;
        if (activeExecutor != null) {
            activeExecutor.execute(this::scanSafely);
        }
    }

    private void scanSafely() {
        if (!running.get()) {
            return;
        }
        try {
            worlds.set(discover());
        } catch (IOException exception) {
            JabeClient.LOGGER.debug("Bedrock LAN discovery failed", exception);
        }
    }

    private List<BedrockLanWorld> discover() throws IOException {
        Map<Long, BedrockLanWorld> discovered = new LinkedHashMap<>();
        byte[] ping = createPing();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(150);

            for (InetAddress address : broadcastAddresses()) {
                socket.send(new DatagramPacket(ping, ping.length, address, DEFAULT_PORT));
            }

            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(900);
            while (System.nanoTime() < deadline) {
                byte[] response = new byte[2048];
                DatagramPacket packet = new DatagramPacket(response, response.length);
                try {
                    socket.receive(packet);
                    parsePong(packet).ifPresent(world -> discovered.merge(
                            world.serverId(), world, BedrockLanDiscovery::preferLanAddress));
                } catch (SocketTimeoutException ignored) {
                    // Keep listening until the complete discovery window has elapsed.
                }
            }
        }

        ArrayList<BedrockLanWorld> result = new ArrayList<>(discovered.values());
        result.sort(Comparator.comparing(BedrockLanWorld::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private byte[] createPing() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + OFFLINE_MESSAGE_ID.length + Long.BYTES)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.put(UNCONNECTED_PING);
        buffer.putLong(System.currentTimeMillis());
        buffer.put(OFFLINE_MESSAGE_ID);
        buffer.putLong(System.nanoTime());
        return buffer.array();
    }

    private List<InetAddress> broadcastAddresses() throws IOException {
        List<InetAddress> addresses = new ArrayList<>();
        addresses.add(InetAddress.getByName("255.255.255.255"));
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) {
            return addresses;
        }

        for (NetworkInterface networkInterface : Collections.list(interfaces)) {
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast != null && !addresses.contains(broadcast)) {
                    addresses.add(broadcast);
                }
            }
        }
        return addresses;
    }

    private java.util.Optional<BedrockLanWorld> parsePong(DatagramPacket packet) {
        byte[] bytes = Arrays.copyOfRange(
                packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
        int headerLength = 1 + Long.BYTES + Long.BYTES + OFFLINE_MESSAGE_ID.length + Short.BYTES;
        if (bytes.length < headerLength || bytes[0] != UNCONNECTED_PONG) {
            return java.util.Optional.empty();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        buffer.get();
        buffer.getLong();
        long serverId = buffer.getLong();
        byte[] magic = new byte[OFFLINE_MESSAGE_ID.length];
        buffer.get(magic);
        if (!Arrays.equals(magic, OFFLINE_MESSAGE_ID)) {
            return java.util.Optional.empty();
        }

        int textLength = Short.toUnsignedInt(buffer.getShort());
        if (textLength > buffer.remaining()) {
            return java.util.Optional.empty();
        }
        byte[] text = new byte[textLength];
        buffer.get(text);
        String[] fields = new String(text, StandardCharsets.UTF_8).split(";", -1);
        if (fields.length < 6 || !"MCPE".equals(fields[0])) {
            return java.util.Optional.empty();
        }

        int advertisedPort = parseInt(fields, 10, packet.getPort());
        InetSocketAddress address = new InetSocketAddress(packet.getAddress(), advertisedPort);
        return java.util.Optional.of(new BedrockLanWorld(
                address,
                value(fields, 1, "Minecraft Bedrock"),
                value(fields, 7, ""),
                value(fields, 3, "unknown"),
                parseInt(fields, 2, -1),
                parseInt(fields, 4, 0),
                parseInt(fields, 5, 0),
                value(fields, 8, "unknown"),
                serverId,
                Instant.now()));
    }

    private static String value(String[] fields, int index, String fallback) {
        return index < fields.length && !fields[index].isBlank() ? fields[index] : fallback;
    }

    private static BedrockLanWorld preferLanAddress(BedrockLanWorld current, BedrockLanWorld candidate) {
        boolean currentIsLan = current.address().getAddress().isSiteLocalAddress();
        boolean candidateIsLan = candidate.address().getAddress().isSiteLocalAddress();
        if (candidateIsLan && !currentIsLan) {
            return candidate;
        }
        return current;
    }

    private static int parseInt(String[] fields, int index, int fallback) {
        if (index >= fields.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(fields[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        ScheduledExecutorService activeExecutor = executor;
        executor = null;
        if (activeExecutor != null) {
            activeExecutor.shutdownNow();
        }
    }
}
