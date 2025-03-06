package mmmfrieddough.craftpilot;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.MinecraftVersion;

public class Reference {
    public static final String MOD_ID = "craftpilot";
    public static final String MOD_NAME = "Craftpilot";
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final int MC_DATA_VERSION = MinecraftVersion.CURRENT.getSaveVersion().getId();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_VERSION;

    static {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        MOD_VERSION = container.getMetadata().getVersion().getFriendlyString();
    }
}
