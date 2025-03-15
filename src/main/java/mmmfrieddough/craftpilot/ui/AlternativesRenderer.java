package mmmfrieddough.craftpilot.ui;

import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class AlternativesRenderer {
    // Constants for styling
    private static final int SQUARE_SIZE = 8;
    private static final int SQUARE_MARGIN = 2;
    private static final int PADDING = 10;
    private static final int ACTIVITY_INDICATOR_WIDTH = 32;
    private static final int ACTIVITY_INDICATOR_HEIGHT = 32;

    // Color constants
    private static final int BORDER_COLOR = 0xFF373737;
    private static final int BACKGROUND_COLOR = 0x998b8b8b;
    private static final int SELECTED_COLOR_ALPHA = 0x99;

    public void render(DrawContext context, ModConfig.Rendering config, MinecraftClient client,
            IWorldManager worldManager) {
        if (!worldManager.hasGhostBlocks()) {
            return;
        }

        int totalAlternatives = worldManager.getTotalAlternativeNum();
        int selected = worldManager.getSelectedAlternativeNum();
        int width = client.getWindow().getScaledWidth();
        int selectedColor = (SELECTED_COLOR_ALPHA << 24) | config.normalOutlineColor;

        // Calculate starting position (first square's left edge, next to activity
        // indicator)
        int startX = width - PADDING - ACTIVITY_INDICATOR_WIDTH - (totalAlternatives * (SQUARE_SIZE + SQUARE_MARGIN))
                + SQUARE_MARGIN;
        // Center vertically with activity indicator
        int y = PADDING + (ACTIVITY_INDICATOR_HEIGHT - SQUARE_SIZE) / 2;

        // Draw squares from left to right
        for (int i = 0; i < totalAlternatives; i++) {
            // Calculate position (moving right from start position)
            int squareX = startX + (i * (SQUARE_SIZE + SQUARE_MARGIN));

            // Fill background
            context.fill(squareX, y, squareX + SQUARE_SIZE, y + SQUARE_SIZE, BACKGROUND_COLOR);

            // Highlight selected alternative
            if (i == selected) {
                context.fill(squareX, y, squareX + SQUARE_SIZE, y + SQUARE_SIZE, selectedColor);
            }

            // Draw border
            context.drawBorder(squareX, y, SQUARE_SIZE, SQUARE_SIZE, BORDER_COLOR);
        }
    }
}
