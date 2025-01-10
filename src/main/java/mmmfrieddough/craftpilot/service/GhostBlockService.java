package mmmfrieddough.craftpilot.service;

import java.util.Map;
import java.util.Optional;

import mmmfrieddough.craftpilot.CraftPilot;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class GhostBlockService {
    public record GhostBlockTarget(BlockPos pos, BlockState state) {
    }

    // Prevent instantiation
    private GhostBlockService() {
    }

    /**
     * Handles ghost block picking interaction
     * 
     * @param camera          Camera instance providing position and view direction
     * @param reach           Maximum interaction range
     * @param enabledFeatures Set of enabled game features
     * @param creativeMode    Whether the player is in creative mode
     * @param inventory       Player's inventory
     * @return true if a ghost block was picked, false otherwise
     */
    public static boolean handleGhostBlockPick(Camera camera, double reach, FeatureSet enabledFeatures,
            boolean creativeMode, PlayerInventory inventory) {
        GhostBlockTarget target = getGhostBlockTarget(camera, reach);
        if (target == null) {
            return false;
        }

        pickGhostBlock(target.state(), enabledFeatures, creativeMode, inventory);
        return true;
    }

    /**
     * Handles ghost block breaking interaction
     * 
     * @param camera Camera instance providing position and view direction
     * @param reach  Maximum interaction range
     * @param player The client player entity
     * @return true if a ghost block was broken, false otherwise
     */
    public static boolean handleGhostBlockBreak(Camera camera, double reach, ClientPlayerEntity player) {
        GhostBlockTarget target = getGhostBlockTarget(camera, reach);
        if (target == null) {
            return false;
        }

        CraftPilot.getInstance().getWorldManager().clearBlockState(target.pos());
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    /**
     * Finds the nearest ghost block along the player's line of sight
     * 
     * @param ghostBlocks Map of ghost blocks and their states
     * @param cameraPos   Starting position for raytrace
     * @param lookVec     Direction vector
     * @param reach       Maximum reach distance
     * @return The nearest ghost block position, or null if none found
     */
    private static BlockPos findTargetedGhostBlock(Map<BlockPos, BlockState> ghostBlocks, Vec3d cameraPos,
            Vec3d lookVec, double reach) {
        if (ghostBlocks.isEmpty()) {
            return null;
        }

        Vec3d endPos = cameraPos.add(lookVec.multiply(reach));
        BlockPos nearestPos = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : ghostBlocks.keySet()) {
            Box box = new Box(pos);
            Optional<Vec3d> hit = box.raycast(cameraPos, endPos);
            if (hit.isPresent()) {
                double dist = cameraPos.squaredDistanceTo(hit.get());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestPos = pos;
                }
            }
        }

        return nearestPos;
    }

    /**
     * Picks the ghost block at the targeted position
     * 
     * @param state           Block state to pick
     * @param enabledFeatures Set of enabled game features
     * @param creativeMode    Whether the player is in creative mode
     * @param inventory       Player's inventory
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
     * Gets the ghost block the player is currently looking at
     * 
     * @param camera Camera instance providing position and view direction
     * @param reach  Maximum interaction range
     * @return A GhostBlockTarget containing the position and state of the targeted
     *         block, or null if none found
     */
    private static GhostBlockTarget getGhostBlockTarget(Camera camera, double reach) {
        Vec3d lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());

        BlockPos targetPos = findTargetedGhostBlock(
                CraftPilot.getInstance().getWorldManager().getGhostBlocks(),
                camera.getPos(),
                lookVec,
                reach);
        if (targetPos == null) {
            return null;
        }

        BlockState ghostState = CraftPilot.getInstance().getWorldManager().getGhostBlocks().get(targetPos);
        return new GhostBlockTarget(targetPos, ghostState);
    }
}