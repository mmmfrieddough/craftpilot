package mmmfrieddough.craftpilot.network;

import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class NetworkManager {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(PlayerPlaceBlockPayload.ID, PlayerPlaceBlockPayload.CODEC);
        ServerNetworking.registerReceivers();
    }
}
