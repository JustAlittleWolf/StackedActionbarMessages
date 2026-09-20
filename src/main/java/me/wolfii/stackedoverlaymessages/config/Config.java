package me.wolfii.stackedoverlaymessages.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.autogen.EnumCycler;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.ListGroup;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class Config {
	private static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
		.id(Identifier.parse("stackedoverlaymessages"))
		.serializer(config -> GsonConfigSerializerBuilder.create(config)
			.setPath(FabricLoader.getInstance().getConfigDir().resolve("stackedoverlaymessages.json"))
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
	public ServerFilterMode serverFilterMode = ServerFilterMode.ALL;

	@AutoGen(category = "servers")
	@ListGroup(valueFactory = StringListFactory.class, controllerFactory = StringListFactory.class)
	@SerialEntry
	public List<String> servers = new ArrayList<>();

	public static Config getConfig() {
		return HANDLER.instance();
	}

	public static Screen createScreen(Screen parent) {
		return HANDLER.generateGui().generateScreen(parent);
	}

	public boolean shouldStack() {
		return this.enabled && this.maxMessages > 1 && this.allowsCurrentServer();
	}

	public int maxHistory() {
		return Math.max(0, this.maxMessages - 1);
	}

	private boolean allowsCurrentServer() {
		if (this.serverFilterMode == ServerFilterMode.ALL) {
			return true;
		}

		String current = currentServerId();
		boolean listed = current != null && this.isListed(current);
		return this.serverFilterMode == ServerFilterMode.WHITELIST ? listed : !listed;
	}

	private boolean isListed(String current) {
		String normalizedCurrent = normalizeServer(current);
		for (String entry : this.servers) {
			if (!entry.isBlank() && normalizeServer(entry).equals(normalizedCurrent)) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable String currentServerId() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hasSingleplayerServer() || minecraft.isLocalServer()) {
			return "singleplayer";
		}
		ServerData server = minecraft.getCurrentServer();
		if (server != null && server.ip != null && !server.ip.isBlank()) {
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
}
