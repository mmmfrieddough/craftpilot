package mmmfrieddough.craftpilot.service;

import java.util.Map;
import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class GhostBlockService {
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
     * Handles inventory picking for a ghost block
     * 
     * @param inventory        Player inventory
     * @param stack            Item to pick
     * @param isCreative       Whether player is in creative mode
     * @param currentHandStack Current item in hand
     * @return Slot that should be selected, or -1 if no action needed
     */
    public static int handleInventoryPick(PlayerInventory inventory, ItemStack stack,
            boolean isCreative, ItemStack currentHandStack) {
        if (isCreative) {
            inventory.addPickBlock(stack);
            return 36 + inventory.selectedSlot;
        } else {
            int slot = inventory.getSlotWithStack(stack);
            if (slot != -1) {
                if (PlayerInventory.isValidHotbarIndex(slot)) {
                    return slot;
                }
                return slot;
            }
        }
        return -1;
    }

    /**
     * Executes the inventory pick action for the given slot
     * 
     * @param client     The Minecraft client instance
     * @param slot       The target inventory slot
     * @param isCreative Whether the player is in creative mode
     */
    public static void executeInventoryPick(MinecraftClient client, int slot, boolean isCreative) {
        if (slot >= 0) {
            if (isCreative) {
                client.interactionManager.clickCreativeStack(
                        client.player.getStackInHand(Hand.MAIN_HAND), slot);
            } else {
                if (slot >= 36) {
                    client.player.getInventory().selectedSlot = slot - 36;
                } else {
                    client.interactionManager.pickFromInventory(slot);
                }
            }
        }
    }
}