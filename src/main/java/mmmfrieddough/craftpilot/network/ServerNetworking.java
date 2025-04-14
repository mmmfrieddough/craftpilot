package mmmfrieddough.craftpilot.network;

import mmmfrieddough.craftpilot.network.payloads.ModHandshakePayload;
import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class ServerNetworking {
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(PlayerPlaceBlockPayload.ID, (payload, context) -> {
            context.server()
                    .execute(() -> handlePlacementPacket(
                            context.server().getWorld(context.player().getEntityWorld().getRegistryKey()),
                            context.player(), payload.hand(), payload.blockPos(), payload.blockState()));
        });

        // Add handler for mod presence check
        ServerPlayNetworking.registerGlobalReceiver(ModHandshakePayload.ID, (payload, context) -> {
            // No need to do anything, the channel being registered is enough
        });
    }

    /**
     * Handles the placement of a block by a player.
     * Tried to keep this as close to the original implementation in
     * ServerPlayNetworkHandler as possible.
     * 
     * @param world      The world the player is in
     * @param player     The player that placed the block
     * @param hand       The hand the player used to place the block
     * @param blockPos   The position the block was placed at
     * @param blockState The state of the block that was placed
     */
    private static void handlePlacementPacket(ServerWorld world, ServerPlayerEntity player, Hand hand,
            BlockPos blockPos, BlockState blockState) {
        ItemStack itemStack;
        // If the player is in creative mode, use the default stack
        if (player.isCreative()) {
            itemStack = blockState.getBlock().asItem().getDefaultStack();
        } else {
            itemStack = player.getStackInHand(hand);
        }

        if (player.isLoaded()) {
            if (itemStack.isItemEnabled(world.getEnabledFeatures())) {
                // The player must be in range or in creative mode
                if (player.canInteractWithBlockAt(blockPos, 1.0) || player.isCreative()) {
                    player.updateLastActionTime();
                    int i = player.getWorld().getTopYInclusive();
                    if (blockPos.getY() <= i) {
                        if (world.canEntityModifyAt(player, blockPos)) {
                            // Encode the position in the block hit result
                            BlockHitResult blockHitResult = new BlockHitResult(player.getPos(),
                                    player.getHorizontalFacing(), blockPos, false);

                            // Set the ghost block state before interacting with the block
                            GhostBlockGlobal.setBlockState(blockState);
                            GhostBlockGlobal.setHandItem(itemStack);
                            GhostBlockGlobal.setActiveHand(hand);
                            ActionResult actionResult = player.interactionManager.interactBlock(player, world,
                                    itemStack, hand, blockHitResult);
                            GhostBlockGlobal.clear();

                            if (actionResult.isAccepted()) {
                                Criteria.ANY_BLOCK_USE.trigger(player, blockPos, itemStack.copy());
                            }

                            if (actionResult instanceof ActionResult.Success success) {
                                if (success.swingSource() == SwingSource.SERVER) {
                                    player.swingHand(hand, true);
                                }
                            }
                        }
                    } else {
                        Text text2 = Text.translatable("build.tooHigh", new Object[] { i }).formatted(Formatting.RED);
                        player.sendMessageToClient(text2, true);
                    }

                    player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, blockPos));
                }
            }
        }
    }
}
