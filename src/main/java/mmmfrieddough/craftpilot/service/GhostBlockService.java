package mmmfrieddough.craftpilot.service;

import java.util.Map;
import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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
     * Finds the nearest ghost block along the player's line of sight
     * 
     * @param ghostBlocks Map of ghost blocks
     * @param cameraPos   Starting position for raytrace
     * @param lookVec     Direction vector
     * @param reach       Maximum reach distance
     * @return The nearest ghost block position, or null if none found
     */
    public static BlockPos findTargetedGhostBlock(Map<BlockPos, BlockState> ghostBlocks, Vec3d cameraPos, Vec3d lookVec,
            double reach) {
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
     * @param client Minecraft client
     * @param state  Block state to pick
     */
    public static void pickGhostBlock(MinecraftClient client, BlockState state) {
        // Get item as stack
        ItemStack itemStack = state.getBlock().asItem().getDefaultStack();

        if (!itemStack.isEmpty()) {
            // Check if item is enabled
            if (itemStack.isItemEnabled(client.player.getWorld().getEnabledFeatures())) {
                PlayerInventory playerInventory = client.player.getInventory();

                // Check if item is already in inventory
                int i = playerInventory.getSlotWithStack(itemStack);
                if (i != -1) {
                    // Select slot if in hotbar, otherwise swap with hotbar
                    if (PlayerInventory.isValidHotbarIndex(i)) {
                        playerInventory.selectedSlot = i;
                    } else {
                        playerInventory.swapSlotWithHotbar(i);
                    }
                } else if (client.player.isInCreativeMode()) {
                    // Add item to inventory
                    playerInventory.swapStackWithHotbar(itemStack);
                    itemStack.setBobbingAnimationTime(5);
                }
            }
        }
    }
}