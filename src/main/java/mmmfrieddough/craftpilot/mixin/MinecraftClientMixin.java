package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;

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
        FeatureSet enabledFeatures = client.player.getWorld().getEnabledFeatures();
        boolean creativeMode = client.interactionManager.getCurrentGameMode().isCreative();
        PlayerInventory inventory = client.player.getInventory();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        ScreenHandler screenHandler = client.player.currentScreenHandler;

        if (GhostBlockService.handleGhostBlockPick(enabledFeatures, creativeMode, inventory, networkHandler,
                screenHandler)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        // Extract necessary information
        IWorldManager worldManager = CraftPilot.getInstance().getWorldManager();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (GhostBlockService.handleGhostBlockBreak(worldManager, player)) {
            cir.setReturnValue(true);
        }
    }
}