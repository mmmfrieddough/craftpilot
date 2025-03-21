package mmmfrieddough.craftpilot.network;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.network.payloads.ModHandshakePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    private static boolean modOnServer = false;

    public static void init() {
        // Register connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Reset flag when joining a server
            modOnServer = false;

            // Try to send a handshake packet - if we can, the server has our mod
            if (ClientPlayNetworking.canSend(ModHandshakePayload.ID)) {
                // We don't actually need to send the packet - just checking if the channel
                // exists
                modOnServer = true;
                CraftPilot.LOGGER.info("CraftPilot detected on server");
            } else {
                CraftPilot.LOGGER.info("CraftPilot not detected on server");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Reset flag when disconnecting
            modOnServer = false;
        });
    }

    public static boolean isModOnServer() {
        return modOnServer;
    }
}