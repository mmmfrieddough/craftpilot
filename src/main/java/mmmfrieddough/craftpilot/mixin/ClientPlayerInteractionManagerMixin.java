package mmmfrieddough.craftpilot.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.service.BlockPlacementService;
import mmmfrieddough.craftpilot.util.GhostBlockGlobal;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "sendSequencedPacket", at = @At("HEAD"), cancellable = true)
    private void onSendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator, CallbackInfo cir) {
        if (GhostBlockGlobal.payload != null) {
            // Call the lambda function like the original method
            packetCreator.predict(0);

            // Send our own packet
            ClientPlayNetworking.send(GhostBlockGlobal.payload);

            // Cancel sending the original packet
            cir.cancel();
        }
    }

    @Inject(method = "interactBlock", at = @At("TAIL"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir) {
        // Check if the mod is enabled
        ModConfig config = CraftPilot.getConfig();
        if (!config.general.enable) {
            return;
        }

        // Check if a block was actually placed
        ActionResult result = cir.getReturnValue();
        if (result == ActionResult.SUCCESS) {
            BlockPlacementService blockPlacementService = CraftPilot.getBlockPlacementService();
            blockPlacementService.onBlockPlaced(hitResult.getBlockPos());
        }
    }
}
