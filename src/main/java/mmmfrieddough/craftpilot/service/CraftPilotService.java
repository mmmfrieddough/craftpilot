package mmmfrieddough.craftpilot.service;

import java.util.HashSet;
import java.util.Set;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.model.ResponseItem;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import mmmfrieddough.craftpilot.world.BlockStateHelper;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CraftPilotService {
    private final IModelConnector modelConnector;
    private final IWorldManager worldManager;
    private final ModConfig config;

    private BlockPos placedBlockPos;
    private boolean blockPlacementPending = false;
    private int nonMatchingBlockCount = 0;
    private int placedBlockCount = 0;
    private Set<BlockPos> ghostBlocksToRemove = new HashSet<>();

    public CraftPilotService(IModelConnector modelConnector, IWorldManager worldManager, ModConfig config) {
        this.modelConnector = modelConnector;
        this.worldManager = worldManager;
        this.config = config;
    }

    public void onBlockPlaced(BlockPos pos) {
        this.ghostBlocksToRemove.add(pos);
    }

    public void onPlayerBlockPlaced(BlockPos pos) {
        this.placedBlockPos = pos;
        this.blockPlacementPending = true;
    }

    private void removeGhostBlocks() {
        for (BlockPos pos : ghostBlocksToRemove) {
            worldManager.clearBlockState(pos);
        }
        ghostBlocksToRemove.clear();
    }

    public void handleWorldTick(World world) {
        if (!shouldProcessTick()) {
            removeGhostBlocks();
            return;
        }
        blockPlacementPending = false;

        // Always stop the current request when user places a block
        modelConnector.stop();

        // Check if the block is replacing a ghost block
        BlockState ghostBlockState = worldManager.getGhostBlockState(placedBlockPos);

        // Remove any ghost blocks that were replaced
        removeGhostBlocks();

        if (ghostBlockState == null) {
            requestNewSuggestions(world, placedBlockPos);
        } else {
            BlockState blockState = world.getBlockState(placedBlockPos);
            processBlockPlacement(world, ghostBlockState, blockState);
        }
    }

    private boolean shouldProcessTick() {
        return blockPlacementPending && placedBlockPos != null;
    }

    private void processBlockPlacement(World world, BlockState ghostBlockState, BlockState blockState) {
        placedBlockCount++;

        if (!ghostBlockState.equals(blockState)) {
            handleNonMatchingBlock(world);
            return;
        }

        if (placedBlockCount >= config.general.placedBlocksThreshold) {
            requestNewSuggestions(world, placedBlockPos);
            resetCounters();
        }
    }

    private void handleNonMatchingBlock(World world) {
        nonMatchingBlockCount++;

        // Clear all suggestions if too many non-matching blocks
        if (nonMatchingBlockCount >= config.general.nonMatchingBlocksThreshold) {
            worldManager.clearBlockStates();
            resetCounters();
            // Request new suggestions after clearing all
            requestNewSuggestions(world, placedBlockPos);
            return;
        }
    }

    private void resetCounters() {
        placedBlockCount = 0;
        nonMatchingBlockCount = 0;
    }

    public void requestNewSuggestions(World world, BlockPos pos) {
        final int offset = config.general.suggestionRange;
        final int size = offset * 2 + 1;
        BlockPos startPos = pos.add(-offset, -offset, -offset);
        String[][][] matrix = getBlocksMatrix(world, startPos, size);
        modelConnector.sendRequest(config.model, matrix, startPos);
    }

    private String[][][] getBlocksMatrix(World world, BlockPos startPos, int size) {
        String[][][] matrix = new String[size][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = startPos.add(x, y, z);
                    BlockState state = worldManager.getBlockState(world, pos);
                    matrix[z][y][x] = BlockMatrixUtils.getBlockStateString(state);
                }
            }
        }
        return matrix;
    }

    public void processResponses() {
        ResponseItem item;
        while ((item = modelConnector.getNextResponse()) != null) {
            try {
                processResponse(item);
            } catch (Exception e) {
                CraftPilot.LOGGER.error("Failed to process response: {}", e.getMessage());
            }
        }
    }

    private void processResponse(ResponseItem item) {
        BlockState blockState = BlockStateHelper.parseBlockState(item.getBlockState());
        if (blockState.isAir()) {
            return;
        }
        BlockPos pos = new BlockPos(item.getX(), item.getY(), item.getZ());
        worldManager.setBlockState(pos, blockState);
    }

    public void cancelCurrentRequest() {
        modelConnector.stop();
    }

    public void clearAll() {
        modelConnector.stop();
        worldManager.clearBlockStates();
    }
}