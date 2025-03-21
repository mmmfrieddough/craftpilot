package mmmfrieddough.craftpilot.network;

import mmmfrieddough.craftpilot.network.payloads.ModHandshakePayload;
import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class NetworkManager {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(PlayerPlaceBlockPayload.ID, PlayerPlaceBlockPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModHandshakePayload.ID, ModHandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModHandshakePayload.ID, ModHandshakePayload.CODEC);
        ServerNetworking.init();
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientNetworking.init();
    }

    @Environment(EnvType.CLIENT)
    public static boolean isModPresentOnServer() {
        return ClientNetworking.isModOnServer();
    }
}
