package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.service.GhostBlockService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.resource.featuretoggle.FeatureSet;

/**
 * Mixin to handle ghost block interactions in the Minecraft client.
 * Provides functionality for picking and breaking ghost blocks.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        // Extract necessary information
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        FeatureSet enabledFeatures = client.player.getWorld().getEnabledFeatures();
        boolean creativeMode = client.interactionManager.getCurrentGameMode().isCreative();
        PlayerInventory inventory = client.player.getInventory();

        if (GhostBlockService.handleGhostBlockPick(camera, reach, enabledFeatures, creativeMode, inventory)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        // Extract necessary information
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        ClientPlayerEntity player = client.player;

        if (GhostBlockService.handleGhostBlockBreak(camera, reach, player)) {
            cir.setReturnValue(true);
        }
    }
}