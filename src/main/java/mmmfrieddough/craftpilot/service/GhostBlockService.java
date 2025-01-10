package mmmfrieddough.craftpilot.service;

import java.util.Map;

import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class GhostBlockService {
    public record GhostBlockTarget(BlockPos pos, BlockState state) {
    }

    // Prevent instantiation
    private GhostBlockService() {
    }

    /**
     * Handles ghost block picking interaction by selecting or adding the targeted
     * block to inventory
     * 
     * @param worldManager    Manager handling ghost block state and interactions
     * @param camera          Camera instance providing position and view direction
     * @param reach           Maximum interaction range in blocks
     * @param enabledFeatures Set of enabled game features
     * @param creativeMode    Whether the player is in creative mode
     * @param inventory       Player's inventory
     * @return true if a ghost block was successfully picked, false otherwise
     */
    public static boolean handleGhostBlockPick(IWorldManager worldManager, Camera camera, double reach,
            FeatureSet enabledFeatures, boolean creativeMode, PlayerInventory inventory, HitResult vanillaTarget) {
        GhostBlockTarget target = getGhostBlockTarget(worldManager, camera, reach, vanillaTarget);
        if (target == null) {
            return false;
        }

        pickGhostBlock(target.state(), enabledFeatures, creativeMode, inventory);
        return true;
    }

    /**
     * Handles ghost block breaking interaction by removing the targeted ghost block
     * 
     * @param worldManager Manager handling ghost block state and interactions
     * @param camera       Camera instance providing position and view direction
     * @param reach        Maximum interaction range in blocks
     * @param player       The client player entity performing the break action
     * @return true if a ghost block was successfully broken, false otherwise
     */
    public static boolean handleGhostBlockBreak(IWorldManager worldManager, Camera camera, double reach,
            ClientPlayerEntity player, HitResult vanillaTarget) {
        GhostBlockTarget target = getGhostBlockTarget(worldManager, camera, reach, vanillaTarget);
        if (target == null) {
            return false;
        }

        worldManager.clearBlockState(target.pos());
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    /**
     * Finds the nearest ghost block along the player's line of sight using
     * raycasting
     * 
     * @param ghostBlocks Map of ghost block positions to their corresponding block
     *                    states
     * @param cameraPos   Starting position for the raycast
     * @param lookVec     Direction vector of the player's view
     * @param reach       Maximum reach distance in blocks
     * @return The position of the nearest ghost block hit by the raycast, or null
     *         if none found
     */
    public static BlockPos findTargetedGhostBlock(Map<BlockPos, BlockState> ghostBlocks, Vec3d cameraPos, Vec3d lookVec,
            double reach, HitResult vanillaTarget) {
        if (ghostBlocks.isEmpty()) {
            return null;
        }

        // Calculate the starting nearest distance
        double nearestDist = vanillaTarget.getType() != HitResult.Type.MISS
                ? cameraPos.squaredDistanceTo(vanillaTarget.getPos())
                : reach * reach;

        BlockPos nearestPos = null;
        Vec3d endPos = cameraPos.add(lookVec.multiply(reach));

        for (BlockPos pos : ghostBlocks.keySet()) {
            BlockState state = ghostBlocks.get(pos);
            VoxelShape shape = state.getOutlineShape(null, pos);
            BlockHitResult hitResult = shape.raycast(cameraPos, endPos, pos);

            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                double dist = cameraPos.squaredDistanceTo(hitResult.getPos());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestPos = pos;
                }
            }
        }

        return nearestPos;
    }

    /**
     * Picks the ghost block by adding or selecting it in the player's inventory
     * 
     * @param state           Block state to pick and convert to item
     * @param enabledFeatures Set of enabled game features for item validation
     * @param creativeMode    Whether the player is in creative mode for inventory
     *                        manipulation
     * @param inventory       Player's inventory to modify
     */
    private static void pickGhostBlock(BlockState state, FeatureSet enabledFeatures, boolean creativeMode,
            PlayerInventory inventory) {
        // Get item as stack
        ItemStack itemStack = state.getBlock().asItem().getDefaultStack();

        if (!itemStack.isEmpty()) {
            // Check if item is enabled
            if (itemStack.isItemEnabled(enabledFeatures)) {
                // Check if item is already in inventory
                int i = inventory.getSlotWithStack(itemStack);
                if (i != -1) {
                    // Select slot if in hotbar, otherwise swap with hotbar
                    if (PlayerInventory.isValidHotbarIndex(i)) {
                        inventory.selectedSlot = i;
                    } else {
                        inventory.swapSlotWithHotbar(i);
                    }
                } else if (creativeMode) {
                    // Add item to inventory
                    inventory.swapStackWithHotbar(itemStack);
                    itemStack.setBobbingAnimationTime(5);
                }
            }
        }
    }

    /**
     * Gets the ghost block the player is currently targeting
     * 
     * @param worldManager Manager handling ghost block state and interactions
     * @param camera       Camera instance providing position and view direction
     * @param reach        Maximum interaction range in blocks
     * @return A GhostBlockTarget containing the position and state of the targeted
     *         block,
     *         or null if no ghost block is being targeted
     */
    private static GhostBlockTarget getGhostBlockTarget(IWorldManager worldManager, Camera camera, double reach,
            HitResult vanillaTarget) {
        Vec3d cameraPos = camera.getPos();
        Vec3d rotationVec = camera.getFocusedEntity().getRotationVec(1.0f);

        // Find nearest ghost block using ray casting
        BlockPos targetPos = findTargetedGhostBlock(worldManager.getGhostBlocks(), cameraPos, rotationVec, reach,
                vanillaTarget);

        if (targetPos == null) {
            return null;
        }

        BlockState ghostState = worldManager.getGhostBlocks().get(targetPos);
        return new GhostBlockTarget(targetPos, ghostState);
    }
}