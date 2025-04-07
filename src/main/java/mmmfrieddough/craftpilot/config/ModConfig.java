package mmmfrieddough.craftpilot.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.ColorPicker;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject;

@Config(name = mmmfrieddough.craftpilot.Reference.MOD_ID)
public class ModConfig implements ConfigData {
    @Category("General")
    @TransitiveObject
    public General general = new General();

    public static class General {
        @Tooltip
        public boolean enableAutoTrigger = true;
        @Tooltip
        @BoundedDiscrete(min = 3, max = 7)
        public int suggestionRange = 5;
        @Tooltip
        public int nonMatchingBlocksThreshold = 3;
        @Tooltip
        public int placedBlocksThreshold = 3;
        @Tooltip
        public boolean enableEasyPlace = true;
        @Tooltip
        public int acceptAllMaxIterations = 5;
        @Tooltip
        public boolean enableInfiniteReach = false;
    }

    @Category("Model")
    @TransitiveObject
    public Model model = new Model();

    public static class Model {
        public enum ModelType {
            DEFAULT("default"),
            IRON("iron"),
            DIAMOND("diamond");

            private final String value;

            ModelType(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        public enum InferenceDevice {
            DEFAULT(""),
            AUTO("auto"),
            CPU("cpu"),
            CUDA("cuda"),
            MPS("mps");

            private final String value;

            InferenceDevice(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        @Tooltip
        public String serverUrl = "http://127.0.0.1:8000/complete-structure/";
        @Tooltip
        @EnumHandler(option = EnumDisplayOption.BUTTON)
        public ModelType modelType = ModelType.DEFAULT;
        @Tooltip
        public String modelVersion = "";
        @Tooltip
        @EnumHandler(option = EnumDisplayOption.BUTTON)
        public InferenceDevice inferenceDevice = InferenceDevice.DEFAULT;
        @Tooltip
        public float temperature = 1.0f;
        @Tooltip
        public int startRadius = 1;
        @Tooltip
        public int maxIterations = 5;
        @Tooltip
        public int maxBlocks = 20;
        @Tooltip
        public int maxAlternatives = 3;
        @Tooltip
        public float minAlternativeProbability = 0.3f;
        @Tooltip
        public boolean ignoreReplaceableBlocks = true;
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
