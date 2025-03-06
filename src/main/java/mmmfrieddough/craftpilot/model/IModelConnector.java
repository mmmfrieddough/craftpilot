package mmmfrieddough.craftpilot.model;

import mmmfrieddough.craftpilot.config.ModConfig;
import net.minecraft.util.math.BlockPos;

public interface IModelConnector {
    public void sendRequest(ModConfig.Model config, String[][][] matrix, BlockPos origin);

    public ResponseItem getNextResponse();

    public void stop();

    public boolean isGenerating();
}
