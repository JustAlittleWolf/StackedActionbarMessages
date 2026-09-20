package me.wolfii.stackedoverlaymessages.mixin;

import java.util.ArrayDeque;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
	private static final int MAX_DISPLAYED = 5;
	private static final int LINE_SPACING = 13;

	@Shadow
	private @Nullable Component overlayMessageString;
	@Shadow
	private int overlayMessageTime;
	@Shadow
	private boolean animateOverlayMessageColor;

	@Shadow
	public abstract Font getFont();

	@Unique
	private final ArrayDeque<StackedOverlay> stackedOverlayMessages$stack = new ArrayDeque<>();

	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void stackedOverlayMessages$push(Component message, boolean animate, CallbackInfo ci) {
		this.stackedOverlayMessages$stack.removeIf(entry -> entry.message.equals(message));
		if (this.overlayMessageString != null && this.overlayMessageTime > 0 && !this.overlayMessageString.equals(message)) {
			this.stackedOverlayMessages$stack.addFirst(
				new StackedOverlay(this.overlayMessageString, this.overlayMessageTime, this.animateOverlayMessageColor)
			);
			while (this.stackedOverlayMessages$stack.size() >= MAX_DISPLAYED) {
				this.stackedOverlayMessages$stack.removeLast();
			}
		}
	}

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void stackedOverlayMessages$tick(CallbackInfo ci) {
		this.stackedOverlayMessages$stack.removeIf(entry -> --entry.time <= 0);
	}

	@Inject(method = "extractOverlayMessage", at = @At("TAIL"))
	private void stackedOverlayMessages$extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (this.stackedOverlayMessages$stack.isEmpty()) {
			return;
		}

		Font font = this.getFont();
		int index = 1;
		for (StackedOverlay entry : this.stackedOverlayMessages$stack) {
			this.stackedOverlayMessages$extractOne(graphics, deltaTracker, font, entry, index * LINE_SPACING);
			index++;
		}
	}

	@Unique
	private void stackedOverlayMessages$extractOne(
		GuiGraphicsExtractor graphics,
		DeltaTracker deltaTracker,
		Font font,
		StackedOverlay entry,
		int yOffset
	) {
		float t = entry.time - deltaTracker.getGameTimeDeltaPartialTick(false);
		int alpha = (int)(t * 255.0F / 20.0F);
		if (alpha > 255) {
			alpha = 255;
		}
		if (alpha <= 0) {
			return;
		}

		graphics.nextStratum();
		graphics.pose().pushMatrix();
		graphics.pose().translate(graphics.guiWidth() / 2, graphics.guiHeight() - 68 + yOffset);
		int color = entry.animate ? Mth.hsvToArgb(t / 50.0F, 0.7F, 0.6F, alpha) : ARGB.white(alpha);
		int width = font.width(entry.message);
		graphics.textWithBackdrop(font, entry.message, -width / 2, -4, width, color);
		graphics.pose().popMatrix();
	}

	@Unique
	private static final class StackedOverlay {
		final Component message;
		int time;
		final boolean animate;

		StackedOverlay(Component message, int time, boolean animate) {
			this.message = message;
			this.time = time;
			this.animate = animate;
		}
	}
}
