package me.wolfii.stackedactionbarmessages.mixin;

import me.wolfii.stackedactionbarmessages.MessageSimilarity;
import me.wolfii.stackedactionbarmessages.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique
    private final ArrayDeque<StackedOverlay> stackedOverlayMessages$stack = new ArrayDeque<>();
    @Shadow
    private @Nullable Component overlayMessageString;
    @Shadow
    private int overlayMessageTime;
    @Shadow
    private boolean animateOverlayMessageColor;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void stackedOverlayMessages$push(Component string, boolean animate, CallbackInfo ci) {
        if (!Config.getConfig().shouldStack()) {
            this.stackedOverlayMessages$stack.clear();
            return;
        }

        String incoming = string.getString();
        this.stackedOverlayMessages$stack.removeIf(entry -> this.stackedOverlayMessages$similar(entry.message, incoming));
        if (this.overlayMessageString != null && this.overlayMessageTime > 0 && !this.stackedOverlayMessages$similar(this.overlayMessageString, incoming)) {
            this.stackedOverlayMessages$stack.addFirst(new StackedOverlay(this.overlayMessageString, this.overlayMessageTime, this.animateOverlayMessageColor));
        }
        this.stackedOverlayMessages$trim();
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void stackedOverlayMessages$tick(CallbackInfo ci) {
        this.stackedOverlayMessages$stack.removeIf(entry -> --entry.time <= 0);
        this.stackedOverlayMessages$trim();
    }

    @Inject(method = "extractOverlayMessage", at = @At("TAIL"))
    private void stackedOverlayMessages$extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Config.getConfig().shouldStack() || this.stackedOverlayMessages$stack.isEmpty()) {
            return;
        }

        int index = 1;
        for (StackedOverlay entry : this.stackedOverlayMessages$stack) {
            this.stackedOverlayMessages$extractOne(graphics, deltaTracker, this.minecraft.font, entry, index * this.minecraft.font.lineHeight + 2);
            index++;
        }
    }

    @Unique
    private boolean stackedOverlayMessages$similar(Component existing, String incoming) {
        return MessageSimilarity.matches(existing.getString(), incoming, Config.getConfig().similarityPercent);
    }

    @Unique
    private void stackedOverlayMessages$trim() {
        while (this.stackedOverlayMessages$stack.size() > Config.getConfig().maxHistory()) {
            this.stackedOverlayMessages$stack.removeLast();
        }
    }

    @Unique
    private void stackedOverlayMessages$extractOne(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Font font, StackedOverlay entry, int yOffset) {
        float t = entry.time - deltaTracker.getGameTimeDeltaPartialTick(false);
        int alpha = (int) (t * 255.0F / 20.0F);
        if (alpha > 255) {
            alpha = 255;
        }
        if (alpha <= 0) {
            return;
        }

        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(graphics.guiWidth() / 2f, graphics.guiHeight() - 68 + yOffset);
        int color = entry.animate ? Mth.hsvToArgb(t / 50.0F, 0.7F, 0.6F, alpha) : ARGB.white(alpha);
        int width = font.width(entry.message);
        graphics.textWithBackdrop(font, entry.message, -width / 2, -4, width, color);
        graphics.pose().popMatrix();
    }

    @Unique
    private static final class StackedOverlay {
        final Component message;
        final boolean animate;
        int time;

        StackedOverlay(Component message, int time, boolean animate) {
            this.message = message;
            this.time = time;
            this.animate = animate;
        }
    }
}
