package dev.jabe.client.network;

import java.net.InetSocketAddress;
import java.time.Instant;

public record BedrockLanWorld(
        InetSocketAddress address,
        String motd,
        String subMotd,
        String gameVersion,
        int protocolVersion,
        int players,
        int maxPlayers,
        String gameMode,
        long serverId,
        Instant lastSeen
) {
    public String displayName() {
        if (subMotd != null && !subMotd.isBlank()) {
            return motd + " - " + subMotd;
        }
        return motd;
    }
}
