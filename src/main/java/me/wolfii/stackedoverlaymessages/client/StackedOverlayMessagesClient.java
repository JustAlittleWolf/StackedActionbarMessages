package me.wolfii.stackedoverlaymessages.client;

import me.wolfii.stackedoverlaymessages.config.Config;
import net.fabricmc.api.ClientModInitializer;

public class StackedOverlayMessagesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.getConfig();
    }
}
