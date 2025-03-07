package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;

@Mixin(BlockItem.class)
public class BlockItemMixinCommon {
    @Shadow
    private Block block;

    @Shadow
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        throw new AssertionError();
    }

    // Override the default block state returned by the getPlacementState method to
    // one from the global state
    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void injectGhostState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        if (GhostBlockGlobal.blockState != null) {
            BlockState originalBlockState = this.block.getPlacementState(context);
            BlockState injectedBlockState = GhostBlockGlobal.blockState;
            cir.setReturnValue(
                    originalBlockState != null && this.canPlace(context, injectedBlockState) ? injectedBlockState
                            : null);
        }
    }
}