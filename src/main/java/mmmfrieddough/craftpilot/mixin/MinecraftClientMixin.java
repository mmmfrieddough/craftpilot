package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Mixin to handle ghost block interactions in the Minecraft client.
 * Provides functionality for picking and breaking ghost blocks.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        GhostBlockTarget target = getGhostBlockTarget();
        if (target == null) {
            return;
        }

        ItemStack stack = target.state().getBlock().getPickStack(client.world, target.pos(), target.state());
        if (!stack.isEmpty()) {
            int slot = GhostBlockService.handleInventoryPick(
                    client.player.getInventory(),
                    stack,
                    client.player.getAbilities().creativeMode,
                    client.player.getStackInHand(Hand.MAIN_HAND));
            GhostBlockService.executeInventoryPick(client, slot, client.player.getAbilities().creativeMode);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        GhostBlockTarget target = getGhostBlockTarget();
        if (target == null) {
            return;
        }

        // Handle breaking the ghost block
        CraftPilot.getInstance().getWorldManager().clearBlockState(target.pos());
        client.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(true);
    }

    private record GhostBlockTarget(BlockPos pos, BlockState state) {
    }

    /**
     * Gets the ghost block the player is currently looking at
     * 
     * @return Target information, or null if no valid target
     */
    private GhostBlockTarget getGhostBlockTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.gameRenderer == null) {
            return null;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);

        BlockPos targetPos = GhostBlockService.findTargetedGhostBlock(
                CraftPilot.getInstance().getWorldManager().getGhostBlocks(), camera.getPos(), lookVec, reach);
        if (targetPos == null) {
            return null;
        }

        BlockState ghostState = CraftPilot.getInstance().getWorldManager().getGhostBlocks().get(targetPos);
        return new GhostBlockTarget(targetPos, ghostState);
    }
}