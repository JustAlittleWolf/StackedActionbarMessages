package me.wolfii.stackedactionbarmessages.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {
    private static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
        .id(Identifier.parse("stackedactionbarmessages"))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("stackedactionbarmessages.json"))
            .build())
        .build();

    static {
        HANDLER.load();
    }

    @AutoGen(category = "general")
    @Boolean(formatter = Boolean.Formatter.ON_OFF)
    @SerialEntry
    public boolean enabled = true;

    @AutoGen(category = "general")
    @IntSlider(min = 1, max = 10, step = 1)
    @SerialEntry
    public int maxMessages = 5;

    @AutoGen(category = "general")
    @IntSlider(min = 0, max = 100, step = 1)
    @SerialEntry
    public int similarityPercent = 80;

    @AutoGen(category = "servers")
    @EnumCycler
    @SerialEntry
    public ServerFilterMode serverFilterMode = ServerFilterMode.NONE;

    @AutoGen(category = "servers")
    @ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
    @SerialEntry
    public List<String> filteredServers = new ArrayList<>();

    public static Config getConfig() {
        return HANDLER.instance();
    }

    public static Screen createScreen(Screen parent) {
        return HANDLER.generateGui().generateScreen(parent);
    }

    private static @Nullable String currentServerId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer() || minecraft.isLocalServer()) {
            return "singleplayer";
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return null;
    }

    private static String normalizeServer(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(":25565")) {
            return normalized.substring(0, normalized.length() - ":25565".length());
        }
        return normalized;
    }

    public boolean shouldStack() {
        return this.enabled && this.maxMessages > 1 && this.allowsCurrentServer();
    }

    public int maxHistory() {
        return Math.max(0, this.maxMessages - 1);
    }

    private boolean allowsCurrentServer() {
        if (this.serverFilterMode == ServerFilterMode.NONE) {
            return true;
        }

        String current = currentServerId();
        boolean listed = current != null && this.isListed(current);
        return (this.serverFilterMode == ServerFilterMode.WHITELIST) == listed;
    }

    private boolean isListed(String current) {
        String normalizedCurrent = normalizeServer(current);
        for (String entry : this.filteredServers) {
            if (!entry.isBlank() && normalizeServer(entry).equals(normalizedCurrent)) {
                return true;
            }
        }
        return false;
    }
}
