package mmmfrieddough.craftpilot.util;

import mmmfrieddough.craftpilot.network.payloads.PlayerPlaceBlockPayload;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class GhostBlockGlobal {
    private static final ThreadLocal<BlockState> blockStateLocal = new ThreadLocal<>();
    private static final ThreadLocal<PlayerPlaceBlockPayload> payloadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> handItemLocal = new ThreadLocal<>();
    private static final ThreadLocal<Hand> activeHandLocal = new ThreadLocal<>();

    // Getters
    public static BlockState getBlockState() {
        return blockStateLocal.get();
    }

    public static PlayerPlaceBlockPayload getPayload() {
        return payloadLocal.get();
    }

    public static ItemStack getHandItem() {
        return handItemLocal.get();
    }

    public static Hand getActiveHand() {
        return activeHandLocal.get();
    }

    // Setters
    public static void setBlockState(BlockState state) {
        blockStateLocal.set(state);
    }

    public static void setPayload(PlayerPlaceBlockPayload payload) {
        payloadLocal.set(payload);
    }

    public static void setHandItem(ItemStack stack) {
        handItemLocal.set(stack);
    }

    public static void setActiveHand(Hand hand) {
        activeHandLocal.set(hand);
    }

    // Clear everything
    public static void clear() {
        blockStateLocal.remove();
        payloadLocal.remove();
        handItemLocal.remove();
        activeHandLocal.remove();
    }
}
