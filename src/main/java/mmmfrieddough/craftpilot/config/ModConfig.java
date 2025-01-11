package mmmfrieddough.craftpilot.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;

@Config(name = mmmfrieddough.craftpilot.Reference.MOD_ID)
public class ModConfig implements ConfigData {
    @Category("General")
    @TransitiveObject
    public General general = new General();

    public static class General {
        @Tooltip
        public boolean enable = true;
        @Tooltip
        public int nonMatchingBlocksThreshold = 3;
        @Tooltip
        public int placedBlocksThreshold = 3;
    }

    @Category("Model")
    @TransitiveObject
    public Model model = new Model();

    public static class Model {
        @Tooltip
        public String serverUrl = "http://127.0.0.1:8000/complete-structure/";
        @Tooltip
        public float temperature = 0.7f;
        @Tooltip
        public int startRadius = 1;
        @Tooltip
        public int maxIterations = 5;
        @Tooltip
        public int maxBlocks = 20;
        @Tooltip
        public float airProbabilityIterationScaling = 0.0f;
    }

    @Category("Rendering")
    @TransitiveObject
    public Rendering rendering = new Rendering();

    public static class Rendering {
        @Tooltip
        public int renderDistance = 128;
        @Tooltip
        public float blockPlacementOpacity = 0.8f;
        @Tooltip
        public float blockOutlineOpacity = 0.4f;
        @Tooltip
        @ColorPicker
        public int normalOutlineColor = 0x00FFFF; // Regular cyan
        @Tooltip
        @ColorPicker
        public int targetedOutlineColor = 0x009FFF; // Blueish cyan
    }
}
