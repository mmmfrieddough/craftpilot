package mmmfrieddough.craftpilot.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * This mixin injects into the BedBlock.onPlaced method.
 * Normally, the bed head block is only placed on the server
 * (wrapped in "if (!world.isClient)"). With this injection, we
 * run the same head-placement logic on the client as well,
 * ensuring the full bed appears immediately so that when we take a snapshot for
 * our suggestions, it includes the full bed.
 * This is how doors already work, so we're just making beds consistent with
 * that.
 */
@Mixin(BedBlock.class)
public class ClientBedBlockMixin {
    @Inject(method = "onPlaced", at = @At("TAIL"))
    private void onPlacedClient(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack itemStack, CallbackInfo ci) {
        // Since the original method only runs on the server, we run the same logic on
        // the client
        if (world.isClient) {
            BlockPos blockPos = pos.offset((Direction) state.get(HorizontalFacingBlock.FACING));
            world.setBlockState(blockPos, (BlockState) state.with(BedBlock.PART, BedPart.HEAD), 3);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, 3);
        }
    }
}
