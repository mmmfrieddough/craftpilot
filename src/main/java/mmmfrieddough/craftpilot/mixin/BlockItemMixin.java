package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    // Override the default block state returned by the getPlacementState method to
    // one from the global state
    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void injectGhostOrientation(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        if (GhostBlockGlobal.blockState != null) {
            cir.setReturnValue(GhostBlockGlobal.blockState);
        }
    }
}