package dev.jabe.client.network;

import dev.jabe.client.JabeClient;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalInt;

/**
 * Optional bridge to ViaFabricPlus/ViaBedrock.
 *
 * <p>Reflection keeps Jabe loadable without ViaFabricPlus while still using
 * its supported connection path when the mod is installed.</p>
 */
public final class BedrockSessionConnector {
    private BedrockSessionConnector() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("viafabricplus");
    }

    public static OptionalInt translatorProtocolVersion() {
        if (!isAvailable()) {
            return OptionalInt.empty();
        }

        try {
            Class<?> bedrockProtocolVersionClass = Class.forName(
                    "net.raphimc.viabedrock.api.BedrockProtocolVersion");
            Object bedrockLatest = bedrockProtocolVersionClass.getField("bedrockLatest").get(null);
            Method getVersion = bedrockLatest.getClass().getMethod("getVersion");
            return OptionalInt.of(((Number) getVersion.invoke(bedrockLatest)).intValue());
        } catch (ReflectiveOperationException | ClassCastException exception) {
            JabeClient.LOGGER.warn("Could not determine ViaBedrock protocol version", exception);
            return OptionalInt.empty();
        }
    }

    public static void connect(BedrockLanWorld world) {
        if (!isAvailable()) {
            throw new IllegalStateException("ViaFabricPlus is not installed");
        }

        String host = world.address().getHostString();
        String address = host.contains(":")
                ? "[" + host + "]:" + world.address().getPort()
                : host + ":" + world.address().getPort();

        try {
            Class<?> protocolVersionClass = Class.forName(
                    "com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Class<?> bedrockProtocolVersionClass = Class.forName(
                    "net.raphimc.viabedrock.api.BedrockProtocolVersion");
            Field latestField = bedrockProtocolVersionClass.getField("bedrockLatest");
            Object bedrockLatest = latestField.get(null);

            Class<?> connectionUtilClass = Class.forName(
                    "com.viaversion.viafabricplus.util.network.ConnectionUtil");
            Method connect = connectionUtilClass.getMethod(
                    "connect", String.class, String.class, protocolVersionClass);

            JabeClient.LOGGER.info("Connecting to Bedrock LAN world {} at {} using ViaBedrock", world.displayName(), address);
            connect.invoke(null, world.displayName(), address, bedrockLatest);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("ViaFabricPlus Bedrock connection API is unavailable", exception);
        }
    }
}
