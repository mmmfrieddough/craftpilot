package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

@Mixin(DoorBlock.class)
public class DoorBlockMixin {
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void overrideDoorPlacement(BlockState state, WorldView world, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        cir.setReturnValue(state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                ? blockState.isSideSolidFullSquare(world, blockPos, Direction.UP)
                : blockState.isOf((Block) (Object) this) && blockState.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER);
    }
}
