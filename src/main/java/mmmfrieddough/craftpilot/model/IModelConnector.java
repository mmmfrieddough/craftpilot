package mmmfrieddough.craftpilot.model;

import mmmfrieddough.craftpilot.config.ModConfig;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public interface IModelConnector {
    public void sendRequest(ModConfig.Model config, int[][][] matrix, Map<Integer, String> palette, BlockPos origin);

    public ResponseItem getNextResponse();

    public void stop();

    public boolean isGenerating();
}
