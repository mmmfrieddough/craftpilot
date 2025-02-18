package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;

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
}
