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
import mmmfrieddough.craftpilot.util.GhostBlockGlobal;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "sendSequencedPacket", at = @At("HEAD"), cancellable = true)
    private void onSendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator, CallbackInfo cir) {
        if (GhostBlockGlobal.payload != null) {
            System.out.println("Sending ghost block packet");
            // Call the lambda function like the original method
            packetCreator.predict(0);

            // Send our own packet
            ClientPlayNetworking.send(GhostBlockGlobal.payload);

            // Cancel sending the original packet
            cir.cancel();
        }
    }
}
