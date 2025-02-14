package mmmfrieddough.craftpilot.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mmmfrieddough.craftpilot.util.GhostBlockGlobal;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "sendSequencedPacket", at = @At("HEAD"), cancellable = true)
    private void onSendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator, CallbackInfo cir) {
        System.out.println("sendSequencedPacket called");
        if (GhostBlockGlobal.payload != null) {
            System.out.println("Ghost block state detected, cancelling packet send");
            // Call the lambda function like the original method
            packetCreator.predict(0);

            // Send our own packet
            ClientPlayNetworking.send(GhostBlockGlobal.payload);

            // Cancel sending the original packet
            cir.cancel();
        }
    }
}
