package mmmfrieddough.craftpilot.service;

import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;

public final class GhostBlockService {
    public record GhostBlockTarget(BlockPos pos, BlockState state) {
    }

    private static GhostBlockTarget currentTarget = null;

    // Prevent instantiation
    private GhostBlockService() {
    }

    /**
     * Handles ghost block picking interaction by selecting or adding the targeted
     * block to inventory
     * 
     * @param enabledFeatures Set of enabled game features
     * @param creativeMode    Whether the player is in creative mode
     * @param inventory       Player's inventory
     * @param networkHandler  Network handler for sending inventory updates
     * @param screenHandler   Current screen handler for the player
     * @return true if a ghost block was successfully picked, false otherwise
     */
    public static boolean handleGhostBlockPick(FeatureSet enabledFeatures, boolean creativeMode,
            PlayerInventory inventory, ClientPlayNetworkHandler networkHandler, ScreenHandler screenHandler) {
        // Get current target
        GhostBlockTarget target = getCurrentTarget();
        if (target == null) {
            return false;
        }

        // Get item as stack
        ItemStack itemStack = target.state.getBlock().asItem().getDefaultStack();

        // Check if item is valid and pick it
        if (!itemStack.isEmpty()) {
            onPickItem(enabledFeatures, creativeMode, inventory, networkHandler, screenHandler, itemStack);
        }

        return true;
    }

    /**
     * Handles ghost block breaking interaction by removing the targeted ghost block
     * 
     * @param worldManager Manager handling ghost block state and interactions
     * @param player       The client player entity performing the break action
     * @return true if a ghost block was successfully broken, false otherwise
     */
    public static boolean handleGhostBlockBreak(IWorldManager worldManager, ClientPlayerEntity player) {
        GhostBlockTarget target = getCurrentTarget();
        if (target == null) {
            return false;
        }

        worldManager.clearBlockState(target.pos());
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    public static boolean handleGhostBlockPlace(MinecraftClient client) {
        GhostBlockTarget target = getCurrentTarget();

        if (target == null || !client.world.getWorldBorder().contains(target.pos())
                || client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        boolean isCreative = client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        // Try both hands
        for (Hand hand : Hand.values()) {
            ItemStack itemStack = client.player.getStackInHand(hand);

            // Special case for creative mode main hand - allow empty hand
            boolean emptyHandCreative = isCreative && hand == Hand.MAIN_HAND && itemStack.isEmpty();
            if (emptyHandCreative) {
                // Temporarily set a fake item stack for the interaction
                itemStack = target.state.getBlock().asItem().getDefaultStack();
                client.player.setStackInHand(hand, itemStack);
            } else if (!itemStack.isItemEnabled(client.world.getEnabledFeatures()) || itemStack.isEmpty()
                    || !itemStack.isOf(target.state.getBlock().asItem())
                    || client.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                continue;
            }

            BlockHitResult blockHitResult = new BlockHitResult(client.player.getPos(),
                    client.player.getHorizontalFacing(), target.pos, false);

            // Set the global ghost block state and payload before interacting
            GhostBlockGlobal.blockState = target.state;
            GhostBlockGlobal.payload = new PlayerPlaceBlockPayload(hand, target.pos, target.state);
            client.interactionManager.interactBlock(client.player, hand, blockHitResult);
            GhostBlockGlobal.blockState = null;
            GhostBlockGlobal.payload = null;

            // Restore empty hand if we temporarily set it
            if (emptyHandCreative) {
                client.player.setStackInHand(hand, ItemStack.EMPTY);
            }

            // Play animation
            client.player.swingHand(hand);
            int previousCount = itemStack.getCount();
            if (!itemStack.isEmpty() && (itemStack.getCount() != previousCount
                    || client.interactionManager.hasCreativeInventory())) {
                client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
            }

            return true;
        }

        return false;
    }

    /**
     * Finds the nearest ghost block along the player's line of sight using
     * raycasting
     * 
     * @param ghostBlocks   Map of ghost block positions to their corresponding
     *                      block
     *                      states
     * @param cameraPos     Starting position for the raycast
     * @param lookVec       Direction vector of the player's view
     * @param reach         Maximum reach distance in blocks
     * @param vanillaTarget The vanilla target result from the client
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
     * Handles picking up a ghost block by selecting or adding the targeted block to
     * the player's inventory
     * 
     * @param features        Set of enabled game features
     * @param creativeMode    Whether the player is in creative mode
     * @param playerInventory Player's inventory
     * @param networkHandler  Network handler for sending inventory updates
     * @param screenHandler   Current screen handler for the player
     * @param stack           The item stack representing the ghost block
     */
    private static void onPickItem(FeatureSet features, boolean creativeMode, PlayerInventory playerInventory,
            ClientPlayNetworkHandler networkHandler, ScreenHandler screenHandler, ItemStack stack) {
        // Check if item is enabled
        if (stack.isItemEnabled(features)) {
            // Check if item is already in inventory
            int i = playerInventory.getSlotWithStack(stack);
            if (i != -1) {
                // Check if item is in hotbar
                if (PlayerInventory.isValidHotbarIndex(i)) {
                    // Select the slot
                    playerInventory.selectedSlot = i;
                } else {
                    // Swap the item with the slot
                    playerInventory.swapSlotWithHotbar(i);
                    networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(),
                            i, playerInventory.selectedSlot, SlotActionType.SWAP, stack, Int2ObjectMaps.emptyMap()));
                }
            } else if (creativeMode) {
                // Add item to inventory
                playerInventory.swapStackWithHotbar(stack);
                stack.setBobbingAnimationTime(5);
                networkHandler
                        .sendPacket(new CreativeInventoryActionC2SPacket(36 + playerInventory.selectedSlot, stack));
            }
            networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
        }
    }

    /**
     * Gets the ghost block the player is currently targeting
     * 
     * @param worldManager  Manager handling ghost block state and interactions
     * @param camera        Camera instance providing position and view direction
     * @param reach         Maximum interaction range in blocks
     * @param vanillaTarget The vanilla target result from the client
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

    public static void updateCurrentTarget(IWorldManager worldManager, Camera camera, double reach,
            HitResult vanillaTarget) {
        currentTarget = getGhostBlockTarget(worldManager, camera, reach, vanillaTarget);
    }

    public static GhostBlockTarget getCurrentTarget() {
        return currentTarget;
    }

    public static BlockPos getCurrentTargetPos() {
        return currentTarget != null ? currentTarget.pos() : null;
    }
}