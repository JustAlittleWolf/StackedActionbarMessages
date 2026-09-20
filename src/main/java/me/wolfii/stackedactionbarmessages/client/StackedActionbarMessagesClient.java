package me.wolfii.stackedactionbarmessages.client;

import me.wolfii.stackedactionbarmessages.config.Config;
import net.fabricmc.api.ClientModInitializer;

public class StackedActionbarMessagesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.getConfig();
    }
}
