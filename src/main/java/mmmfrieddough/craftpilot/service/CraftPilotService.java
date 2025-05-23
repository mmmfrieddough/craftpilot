package mmmfrieddough.craftpilot.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.model.IModelConnector;
import mmmfrieddough.craftpilot.model.ResponseItem;
import mmmfrieddough.craftpilot.network.NetworkManager;
import mmmfrieddough.craftpilot.service.GhostBlockService.GhostBlockTarget;
import mmmfrieddough.craftpilot.util.BlockMatrixUtils;
import mmmfrieddough.craftpilot.world.BlockStateHelper;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

    public void acceptAll(MinecraftClient client) {
        // Validate creative mode
        if (!client.interactionManager.getCurrentGameMode().isCreative()) {
            client.player.sendMessage(
                    Text.translatable("message.craftpilot.creative_required_for_accept_all").formatted(Formatting.RED),
                    true);
            return;
        }

        // Validate mod presence on server
        if (!NetworkManager.isModPresentOnServer()) {
            client.player.sendMessage(
                    Text.translatable("message.craftpilot.server_required_for_accept_all").formatted(Formatting.RED),
                    true);
            return;
        }

        cancelSuggestions();
        worldManager.pruneAlternatives();
        GhostBlockService.handleGhostBlockPlaceAll(client, worldManager, config.general.acceptAllMaxIterations);
    }

    /**
     * Gets the blocks matrix with a palette for efficient storage.
     * 
     * @param world    The world to get blocks from
     * @param startPos The starting position
     * @param size     The size of the matrix (in each dimension)
     * @return A record containing the block matrix of indices and a palette mapping
     *         indices to block state strings
     */
    private BlockMatrixWithPalette getBlocksMatrix(World world, BlockPos startPos, int size) {
        int[][][] matrix = new int[size][size][size];
        Map<String, Integer> blockToIndex = new HashMap<>();
        Map<Integer, String> palette = new HashMap<>();
        int nextIndex = 0;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    BlockPos pos = startPos.add(x, y, z);
                    BlockState state = worldManager.getBlockState(world, pos);
                    String blockStateString = BlockMatrixUtils.getBlockStateString(state);

                    // Get or create palette index
                    Integer index = blockToIndex.get(blockStateString);
                    if (index == null) {
                        index = nextIndex++;
                        blockToIndex.put(blockStateString, index);
                        palette.put(index, blockStateString);
                    }

                    matrix[z][y][x] = index;
                }
            }
        }

        return new BlockMatrixWithPalette(matrix, palette);
    }

    /**
     * Record to store a block matrix with its palette.
     */
    private record BlockMatrixWithPalette(int[][][] matrix, Map<Integer, String> palette) {
    }

    private void requestSuggestions(World world, BlockPos pos) {
        final int offset = config.general.suggestionRange;
        final int size = offset * 2 + 1;
        BlockPos startPos = pos.add(-offset, -offset, -offset);
        worldManager.pruneAlternatives();
        BlockMatrixWithPalette matrixWithPalette = getBlocksMatrix(world, startPos, size);
        modelConnector.sendRequest(config.model, matrixWithPalette.matrix(), matrixWithPalette.palette(), startPos);
        resetCounters();
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
        Vec3d vec3d = hitResult.getPos();
        if (!vec3d.isInRange(cameraPos, interactionRange)) {
            Vec3d vec3d2 = hitResult.getPos();
            Direction direction = Direction.getFacing(vec3d2.x - cameraPos.x, vec3d2.y - cameraPos.y,
                    vec3d2.z - cameraPos.z);
            return BlockHitResult.createMissed(vec3d2, direction, BlockPos.ofFloored(vec3d2));
        } else {
            return hitResult;
        }
    }

    public static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange,
            double entityInteractionRange,
            float tickDelta) {
        double d = Math.max(blockInteractionRange, entityInteractionRange);
        double e = MathHelper.square(d);
        Vec3d vec3d = camera.getCameraPosVec(tickDelta);
        HitResult hitResult = camera.raycast(d, tickDelta, false);
        double f = hitResult.getPos().squaredDistanceTo(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            e = f;
            d = Math.sqrt(f);
        }

        Vec3d vec3d2 = camera.getRotationVec(tickDelta);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        Box box = camera.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(camera, vec3d, vec3d3, box, EntityPredicates.CAN_HIT,
                e);
        return entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(vec3d) < f
                ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange)
                : ensureTargetInRange(hitResult, vec3d, blockInteractionRange);
    }

    private BlockPos getTargetBlockPosition(MinecraftClient client) {
        // First check for ghost block target
        GhostBlockTarget target = GhostBlockService.getCurrentTarget();
        if (target != null) {
            return target.pos();
        }

        // Fall back to regular crosshair target
        Entity cameraEntity = client.getCameraEntity();
        double blockInteractionRange = config.general.enableInfiniteReach ? 10000.0D
                : client.player.getBlockInteractionRange();
        double entityInteractionRange = client.player.getEntityInteractionRange();
        BlockHitResult hitResult = (BlockHitResult) findCrosshairTarget(cameraEntity, blockInteractionRange,
                entityInteractionRange, 1.0F);
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        return null;
    }

    private void removeGhostBlocks() {
        for (BlockPos pos : ghostBlocksToRemove) {
            worldManager.clearBlockStateAllAlternatives(pos);
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

    private void processResponse(ResponseItem item) {
        BlockState blockState = BlockStateHelper.parseBlockState(item.getBlockState());
        BlockPos pos = new BlockPos(item.getX(), item.getY(), item.getZ());
        worldManager.setBlockState(item.getAlternativeNum(), item.getPreviousAlternativeNum(), pos, blockState);
    }
}