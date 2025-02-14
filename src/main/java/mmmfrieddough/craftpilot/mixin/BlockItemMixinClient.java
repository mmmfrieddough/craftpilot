package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.service.BlockPlacementService;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(BlockItem.class)
public class BlockItemMixinClient {
    @Inject(method = "place", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onBlockPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir,
            ItemPlacementContext context2, BlockState blockState, BlockPos blockPos, World world) {
        // This should only run on the client
        if (!world.isClient()) {
            return;
        }

        // Check if the mod is enabled
        ModConfig config = CraftPilot.getConfig();
        if (!config.general.enable) {
            return;
        }

        // Check if a block was actually placed
        if (cir.getReturnValue().isAccepted()) {
            BlockPlacementService blockPlacementService = CraftPilot.getBlockPlacementService();
            blockPlacementService.onBlockPlaced(blockPos);
        }
    }
}
