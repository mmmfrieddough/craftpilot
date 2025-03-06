package mmmfrieddough.craftpilot.service;

import java.util.HashSet;
import java.util.Set;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.model.ResponseItem;
import mmmfrieddough.craftpilot.service.GhostBlockService.GhostBlockTarget;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import mmmfrieddough.craftpilot.world.BlockStateHelper;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
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

    /**
     * Called when any block is placed in the world.
     * This method marks ghost blocks for removal when they are replaced by real
     * blocks,
     * ensuring that ghost blocks don't remain visible after a block has been placed
     * at the same position.
     * 
     * @param pos The position where the block was placed
     */
    public void onBlockPlaced(BlockPos pos) {
        this.ghostBlocksToRemove.add(pos);
    }

    /**
     * Called when a player places a block in the world.
     * This method sets the position of the placed block and marks that a block
     * placement is pending.
     * 
     * @param pos The position where the block was placed
     */
    public void onPlayerBlockPlaced(BlockPos pos) {
        this.placedBlockPos = pos;
        this.blockPlacementPending = true;
    }

    /**
     * Processes pending block placements.
     * This method processes pending block placements by checking if the block is
     * replacing a ghost block or if it is placed in an empty space.
     * If the block is replacing a ghost block, the block states are processed
     * immediately.
     * If the block is placed in an empty space, new suggestions are requested from
     * the model.
     * 
     * @param world The world to process block placements in
     */
    public void processPendingBlockPlacements(World world) {
        if (!shouldProcessPlacement()) {
            removeGhostBlocks();
            return;
        }
        blockPlacementPending = false;

        // Always stop the current request when there is a change to process
        modelConnector.stop();

        // Check if the block is replacing a ghost block
        BlockState ghostBlockState = worldManager.getGhostBlockState(placedBlockPos);

        // Remove any ghost blocks that were replaced
        removeGhostBlocks();

        if (ghostBlockState == null) {
            // Request new suggestions if the block is placed in an empty space
            requestSuggestions(world, placedBlockPos);
        } else {
            // Process the block placement if the block is replacing a ghost block
            BlockState blockState = world.getBlockState(placedBlockPos);
            processBlockPlacement(world, ghostBlockState, blockState);
        }
    }

    /**
     * Triggers suggestions for the block at the crosshair target.
     * This method triggers suggestions for the block at the crosshair target by
     * requesting suggestions from the model connector.
     * If the crosshair target is a ghost block, the suggestions are requested for
     * the ghost block position.
     * 
     * @param client The Minecraft client
     */
    public void triggerSuggestions(MinecraftClient client) {
        BlockPos pos = getTargetBlockPosition(client);
        if (pos != null) {
            cancelSuggestions();
            requestSuggestions(client.world, pos);
        }
    }

    /**
     * Process responses from the model connector.
     * This method processes all responses from the model connector and sets the
     * block states in the world manager.
     */
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

    /**
     * Cancels the current suggestions request.
     * This method stops the model connector from processing the current request.
     */
    public void cancelSuggestions() {
        modelConnector.stop();
    }

    /**
     * Clears all block states.
     * This method clears all block states in the world manager and cancels any
     * pending suggestions requests.
     */
    public void clearAll() {
        cancelSuggestions();
        worldManager.clearBlockStates();
    }

    /**
     * Requests suggestions from the model for the given world and position.
     * This method sends a request to the model connector with the block matrix
     * around the given position.
     * 
     * @param world The world to request suggestions for
     * @param pos   The position to request suggestions around
     */
    private void requestSuggestions(World world, BlockPos pos) {
        final int offset = config.general.suggestionRange;
        final int size = offset * 2 + 1;
        BlockPos startPos = pos.add(-offset, -offset, -offset);
        String[][][] matrix = getBlocksMatrix(world, startPos, size);
        modelConnector.sendRequest(config.model, matrix, startPos);
        resetCounters();
    }

    private BlockPos getTargetBlockPosition(MinecraftClient client) {
        // First check for ghost block target
        GhostBlockTarget target = GhostBlockService.getCurrentTarget();
        if (target != null) {
            return target.pos();
        }

        // Fall back to regular crosshair target
        BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        return null;
    }

    private void removeGhostBlocks() {
        for (BlockPos pos : ghostBlocksToRemove) {
            worldManager.clearBlockState(pos);
        }
        ghostBlocksToRemove.clear();
    }

    private boolean shouldProcessPlacement() {
        return blockPlacementPending && placedBlockPos != null;
    }

    private void processBlockPlacement(World world, BlockState ghostBlockState, BlockState blockState) {
        placedBlockCount++;

        if (!ghostBlockState.equals(blockState)) {
            handleNonMatchingBlock(world);
            return;
        }

        if (placedBlockCount >= config.general.placedBlocksThreshold) {
            requestSuggestions(world, placedBlockPos);
        }
    }

    private void handleNonMatchingBlock(World world) {
        nonMatchingBlockCount++;

        // Clear all suggestions if too many non-matching blocks
        if (nonMatchingBlockCount >= config.general.nonMatchingBlocksThreshold) {
            worldManager.clearBlockStates();
            // Request new suggestions after clearing all
            requestSuggestions(world, placedBlockPos);
            return;
        }
    }

    private void resetCounters() {
        placedBlockCount = 0;
        nonMatchingBlockCount = 0;
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

    private void processResponse(ResponseItem item) {
        BlockState blockState = BlockStateHelper.parseBlockState(item.getBlockState());
        if (blockState.isAir()) {
            return;
        }
        BlockPos pos = new BlockPos(item.getX(), item.getY(), item.getZ());
        worldManager.setBlockState(pos, blockState);
    }
}