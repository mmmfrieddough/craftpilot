package mmmfrieddough.craftpilot.service;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.http.HttpService;
import mmmfrieddough.craftpilot.http.ResponseItem;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import mmmfrieddough.craftpilot.world.BlockStateHelper;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockPlacementService {
    private static final int MATRIX_SIZE = 11;
    private static final int MATRIX_OFFSET = MATRIX_SIZE / 2;

    private final HttpService httpService;
    private final IWorldManager worldManager;
    private final ModConfig config;

    private BlockPos placedBlockPos;
    private boolean blockPlacementPending = false;
    private int nonMatchingBlockCount = 0;
    private int placedBlockCount = 0;

    public BlockPlacementService(HttpService httpService, IWorldManager worldManager, ModConfig config) {
        this.httpService = httpService;
        this.worldManager = worldManager;
        this.config = config;
    }

    public void onBlockPlaced(BlockPos pos) {
        this.placedBlockPos = pos;
        this.blockPlacementPending = true;
    }

    public void handleWorldTick(World world) {
        if (!shouldProcessTick()) {
            return;
        }
        blockPlacementPending = false;

        // Always stop the current request when user places a block
        httpService.stop();

        // Check if the block is replacing a ghost block
        BlockState ghostBlockState = worldManager.getGhostBlockState(placedBlockPos);
        if (ghostBlockState == null) {
            requestNewSuggestions(world);
            return;
        }

        BlockState blockState = world.getBlockState(placedBlockPos);
        processBlockPlacement(world, ghostBlockState, blockState);
    }

    private boolean shouldProcessTick() {
        return blockPlacementPending && placedBlockPos != null;
    }

    private void processBlockPlacement(World world, BlockState ghostBlockState, BlockState blockState) {
        placedBlockCount++;
        worldManager.clearBlockState(placedBlockPos);

        if (!ghostBlockState.equals(blockState)) {
            handleNonMatchingBlock(world);
            return;
        }

        if (placedBlockCount >= config.general.placedBlocksThreshold) {
            requestNewSuggestions(world);
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
            requestNewSuggestions(world);
            return;
        }
    }

    private void resetCounters() {
        placedBlockCount = 0;
        nonMatchingBlockCount = 0;
    }

    private void requestNewSuggestions(World world) {
        String[][][] matrix = getBlocksMatrix(world, placedBlockPos);
        httpService.sendRequest(config.model, matrix, placedBlockPos);
    }

    private String[][][] getBlocksMatrix(World world, BlockPos centerPos) {
        String[][][] matrix = new String[MATRIX_SIZE][MATRIX_SIZE][MATRIX_SIZE];
        for (int x = 0; x < MATRIX_SIZE; x++) {
            for (int y = 0; y < MATRIX_SIZE; y++) {
                for (int z = 0; z < MATRIX_SIZE; z++) {
                    BlockPos pos = centerPos.add(
                            x - MATRIX_OFFSET,
                            y - MATRIX_OFFSET,
                            z - MATRIX_OFFSET);
                    BlockState state = worldManager.getBlockState(world, pos);
                    matrix[z][y][x] = BlockMatrixUtils.getBlockStateString(state);
                }
            }
        }
        return matrix;
    }

    public void processResponses() {
        ResponseItem item;
        while ((item = httpService.getNextResponse()) != null) {
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
        httpService.stop();
    }

    public void clearAll() {
        httpService.stop();
        worldManager.clearBlockStates();
    }
}