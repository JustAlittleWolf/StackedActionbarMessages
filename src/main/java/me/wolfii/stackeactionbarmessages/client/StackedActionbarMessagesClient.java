package me.wolfii.stackeactionbarmessages.client;

import me.wolfii.stackeactionbarmessages.config.Config;
import net.fabricmc.api.ClientModInitializer;

public class StackedActionbarMessagesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.getConfig();
    }
}
