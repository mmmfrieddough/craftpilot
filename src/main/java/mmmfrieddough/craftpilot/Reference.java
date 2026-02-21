package mmmfrieddough.craftpilot;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.GameVersion;
import net.minecraft.SharedConstants;

public class Reference {
    public static final String MOD_ID = "craftpilot";
    public static final String MOD_NAME = "Craftpilot";

    private static final GameVersion GAME_VERSION = SharedConstants.getGameVersion();
    public static final String MC_VERSION = GAME_VERSION.name();
    public static final int MC_DATA_VERSION = GAME_VERSION.dataVersion().id();

    public static final String MOD_TYPE = "fabric";
    public static final String MOD_VERSION;

    static {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        MOD_VERSION = container.getMetadata().getVersion().getFriendlyString();
    }
}
