package dev.jabe.client;

import dev.jabe.client.network.BedrockLanDiscovery;
import dev.jabe.client.screen.BedrockLanScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public final class JabeClient implements ClientModInitializer {
    public static final String MOD_ID = "jabe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final AtomicReference<JoinMultiplayerScreen> PENDING_BEDROCK_SCREEN =
            new AtomicReference<>();

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof JoinMultiplayerScreen)) {
                return;
            }

            Screens.getWidgets(screen).add(Button.builder(
                    Component.translatable("jabe.multiplayer.bedrock_lan"),
                    button -> PENDING_BEDROCK_SCREEN.set((JoinMultiplayerScreen) screen))
                    .bounds(8, 8, 118, 20)
                    .build());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            JoinMultiplayerScreen parent = PENDING_BEDROCK_SCREEN.getAndSet(null);
            if (parent != null && client.screen == parent) {
                LOGGER.info("Opening Bedrock LAN discovery screen");
                client.setScreen(new BedrockLanScreen(parent, BedrockLanDiscovery.getInstance()));
            }
        });

        LOGGER.info("Jabe initialized for Minecraft 26.1.2");
    }
}
