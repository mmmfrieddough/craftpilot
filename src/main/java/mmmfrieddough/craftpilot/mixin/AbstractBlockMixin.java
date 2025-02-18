package mmmfrieddough.craftpilot.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void overrideBedPlacement(BlockState state, WorldView world, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof BedBlock) {
            // The head block can only be placed if the corresponding foot block is already
            // placed
            BlockPos blockPos = pos.offset(state.get(HorizontalFacingBlock.FACING).getOpposite());
            BlockState blockState = world.getBlockState(blockPos);
            cir.setReturnValue(state.get(BedBlock.PART) == BedPart.FOOT ? true
                    : blockState.isOf((Block) (Object) this) && blockState.get(BedBlock.PART) == BedPart.FOOT);
        }
    }
}