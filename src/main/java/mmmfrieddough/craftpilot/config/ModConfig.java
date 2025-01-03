package mmmfrieddough.craftpilot.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;

@Config(name = mmmfrieddough.craftpilot.Reference.MOD_ID)
public class ModConfig implements ConfigData {
    @Category("Client")
    @TransitiveObject
    public Client client = new Client();

    public static class Client {
        @Tooltip
        public boolean enable = true;
    }

    @Category("Model")
    @TransitiveObject
    public Model model = new Model();

    public static class Model {
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
        public float blockPlacementOpacity = 0.8f;
        @Tooltip
        public float blockOutlineOpacity = 0.4f;
    }

}
