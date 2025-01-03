package mmmfrieddough.craftpilot.schematic;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class SchematicManager {
    private static final int MATRIX_SIZE = 11;

    // Simplified state storage
    private final Map<BlockPos, BlockState> ghostBlocks = new HashMap<>();
    private BlockPos originPos;

    public void createSchematic(World world, BlockPos pos) {
        this.originPos = pos;
        this.ghostBlocks.clear();
    }

    public void processResponse(ResponseItem response) {
        if (originPos == null) {
            CraftPilot.LOGGER.error("No origin position set");
            return;
        }

        try {
            BlockState blockState = BlockStateHelper.parseBlockState(response.getBlockState());
            BlockPos pos = originPos.add(
                    response.getX() - 5,
                    response.getY() - 5,
                    response.getZ() - 5);
            CraftPilot.LOGGER.info("Setting block at {} to {}", pos, blockState);
            ghostBlocks.put(pos, blockState);

            // Trigger a visual update
            MinecraftClient.getInstance().worldRenderer.updateBlock(null, pos, null, null, 0);
        } catch (Exception e) {
            CraftPilot.LOGGER.error("Error processing response", e);
        }
    }

    // Add this method to hook into Minecraft's rendering
    public boolean shouldRenderGhostBlock(BlockPos pos, BlockState currentState) {
        BlockState ghostState = ghostBlocks.get(pos);
        return ghostState != null && ghostState != currentState;
    }

    public BlockState getGhostBlockState(BlockPos pos) {
        return ghostBlocks.get(pos);
    }

    public Map<BlockPos, BlockState> getGhostBlocks() {
        return Collections.unmodifiableMap(ghostBlocks);
    }

    public String[][][] getBlocksMatrix(World world, BlockPos centerPos) {
        int offset = MATRIX_SIZE / 2;
        String[][][] matrix = new String[MATRIX_SIZE][MATRIX_SIZE][MATRIX_SIZE];

        for (int x = 0; x < MATRIX_SIZE; x++) {
            for (int y = 0; y < MATRIX_SIZE; y++) {
                for (int z = 0; z < MATRIX_SIZE; z++) {
                    BlockPos pos = centerPos.add(x - offset, y - offset, z - offset);
                    BlockState state = world.getBlockState(pos);
                    matrix[z][y][x] = BlockMatrixUtils.getBlockStateString(state);
                }
            }
        }

        return matrix;
    }
}