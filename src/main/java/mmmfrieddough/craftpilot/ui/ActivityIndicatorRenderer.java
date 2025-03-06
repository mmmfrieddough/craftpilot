package mmmfrieddough.craftpilot.ui;

import mmmfrieddough.craftpilot.Reference;
import mmmfrieddough.craftpilot.model.IModelConnector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ActivityIndicatorRenderer {
    // Define the texture resource
    private static final Identifier INDICATOR_TEXTURE = Identifier.of(Reference.MOD_ID,
            "textures/gui/activity_indicator.png");

    // Animation properties
    private static final float MAX_ALPHA = 0.6f;
    private static final float MIN_ALPHA = 0.2f;
    private static final int FADE_MS = 1000;
    private long fadeStartTime = 0;

    // Image size
    private static final int IMG_WIDTH = 32;
    private static final int IMG_HEIGHT = 32;

    private float calculateAlpha() {
        // Calculate alpha based on elapsed time
        long currentTime = Util.getMeasuringTimeMs();
        long elapsedMs = currentTime - fadeStartTime;
        float elapsedCycles = (float) elapsedMs / FADE_MS;
        // Sine wave from 0 to 1 to 0
        float progress = 0.5f * (1.0f + (float) Math.sin((elapsedCycles + 0.5) * Math.PI));
        return MIN_ALPHA + progress * (MAX_ALPHA - MIN_ALPHA);
    }

    private void renderImage(DrawContext context, MinecraftClient client, float alpha) {
        // Calculate position (top right corner with some padding)
        int width = client.getWindow().getScaledWidth();
        int x = width - IMG_WIDTH - 10;
        int y = 10;

        // Draw with appropriate opacity
        int alphaInt = (int) (alpha * 255.0f);
        int color = (alphaInt << 24) | 0xFFFFFF; // ARGB format

        // Draw the texture
        context.drawTexture(RenderLayer::getGuiTextured, INDICATOR_TEXTURE, x, y, 0.0f, 0.0f, IMG_WIDTH, IMG_HEIGHT,
                IMG_WIDTH, IMG_HEIGHT, color);
    }

    public void render(DrawContext context, MinecraftClient client, IModelConnector modelConnector,
            RenderTickCounter tickDelta) {
        if (!modelConnector.isGenerating()) {
            fadeStartTime = 0;
            return;
        }

        if (fadeStartTime == 0) {
            fadeStartTime = Util.getMeasuringTimeMs();
        }

        float alpha = calculateAlpha();

        // Only render if we have some visibility
        if (alpha > 0.01f) {
            renderImage(context, client, alpha);
        }
    }
}