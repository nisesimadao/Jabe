package dev.jabe.client.screen;

import dev.jabe.client.compat.CompatibilityState;
import dev.jabe.client.network.BedrockLanDiscovery;
import dev.jabe.client.network.BedrockLanWorld;
import dev.jabe.client.network.BedrockSessionConnector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.OptionalInt;

public final class BedrockLanScreen extends Screen {
    private final Screen parent;
    private final BedrockLanDiscovery discovery;
    private List<BedrockLanWorld> renderedWorlds = List.of();
    private Component status = Component.translatable("jabe.lan.searching");

    public BedrockLanScreen(Screen parent, BedrockLanDiscovery discovery) {
        super(Component.translatable("jabe.lan.title"));
        this.parent = parent;
        this.discovery = discovery;
    }

    @Override
    protected void init() {
        dev.jabe.client.JabeClient.LOGGER.info("Bedrock LAN discovery screen initialized");
        discovery.start();
        discovery.refreshNow();
        rebuildWorldButtons();
    }

    @Override
    public void tick() {
        List<BedrockLanWorld> current = discovery.snapshot();
        if (!sameWorlds(current, renderedWorlds)) {
            rebuildWorldButtons();
        }
    }

    private static boolean sameWorlds(List<BedrockLanWorld> left, List<BedrockLanWorld> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            BedrockLanWorld a = left.get(index);
            BedrockLanWorld b = right.get(index);
            if (!a.address().equals(b.address())
                    || !a.motd().equals(b.motd())
                    || !a.subMotd().equals(b.subMotd())
                    || !a.gameVersion().equals(b.gameVersion())
                    || a.protocolVersion() != b.protocolVersion()
                    || a.players() != b.players()
                    || a.maxPlayers() != b.maxPlayers()
                    || !a.gameMode().equals(b.gameMode())
                    || a.serverId() != b.serverId()) {
                return false;
            }
        }
        return true;
    }

    private void rebuildWorldButtons() {
        clearWidgets();
        renderedWorlds = discovery.snapshot();

        int y = 52;
        for (BedrockLanWorld world : renderedWorlds.stream().limit(8).toList()) {
            Component label = Component.literal(world.displayName() + "  "
                    + world.players() + "/" + world.maxPlayers() + "  "
                    + world.gameVersion());
            addRenderableWidget(Button.builder(label, button -> select(world))
                    .bounds(width / 2 - 160, y, 320, 22)
                    .build());
            y += 25;
        }

        addRenderableWidget(Button.builder(Component.translatable("jabe.lan.refresh"), button -> {
                    status = Component.translatable("jabe.lan.searching");
                    discovery.refreshNow();
                })
                .bounds(width / 2 - 154, height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 + 54, height - 28, 100, 20)
                .build());

        status = renderedWorlds.isEmpty()
                ? Component.translatable("jabe.lan.none")
                : Component.translatable("jabe.lan.found", renderedWorlds.size());
    }

    private void select(BedrockLanWorld world) {
        CompatibilityState.getInstance().enterBedrockMode();
        if (!BedrockSessionConnector.isAvailable()) {
            status = Component.translatable("jabe.lan.bridge_unavailable");
            return;
        }

        OptionalInt translatorProtocol = BedrockSessionConnector.translatorProtocolVersion();
        if (translatorProtocol.isPresent() && translatorProtocol.getAsInt() != world.protocolVersion()) {
            status = Component.translatable(
                    "jabe.lan.protocol_unsupported", world.protocolVersion(), translatorProtocol.getAsInt());
            return;
        }

        status = Component.translatable("jabe.lan.connecting", world.address().getHostString());
        try {
            BedrockSessionConnector.connect(world);
        } catch (RuntimeException exception) {
            dev.jabe.client.JabeClient.LOGGER.error("Could not connect to Bedrock LAN world", exception);
            status = Component.translatable("jabe.lan.connect_failed", exception.getMessage());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 18, 0xffffffff);
        graphics.centeredText(font, status, width / 2, 35, 0xffa0a0a0);
    }

    @Override
    public void onClose() {
        CompatibilityState.getInstance().enterJavaMode();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        discovery.close();
    }
}
