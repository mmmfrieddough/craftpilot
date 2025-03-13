package mmmfrieddough.craftpilot.ui;

import mmmfrieddough.craftpilot.Reference;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.world.IWorldManager;
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
    private static final float FADE_MS = 1000;
    private long previousTime = 0;
    private boolean fadingIn = false;
    private float alpha = MIN_ALPHA;

    // Image size
    private static final int IMG_WIDTH = 32;
    private static final int IMG_HEIGHT = 32;

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
            IWorldManager worldManager, RenderTickCounter tickDelta) {
        long currentTime = Util.getMeasuringTimeMs();
        float alphaChange = (currentTime - previousTime) / FADE_MS;
        boolean generating = modelConnector.isGenerating();
        boolean hasGhostBlocks = worldManager.hasGhostBlocks();
        float minAlpha = generating || hasGhostBlocks ? MIN_ALPHA : 0.0f;
        if (generating && fadingIn || !generating && hasGhostBlocks) {
            alpha = Math.min(MAX_ALPHA, alpha + alphaChange);
            if (alpha == MAX_ALPHA) {
                fadingIn = false;
            }
        } else {
            alpha = Math.max(minAlpha, alpha - alphaChange);
            if (alpha == minAlpha) {
                fadingIn = true;
            }
        }
        renderImage(context, client, alpha);
        previousTime = currentTime;
    }
}