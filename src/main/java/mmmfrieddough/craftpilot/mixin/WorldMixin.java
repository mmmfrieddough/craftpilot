package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilotClient;
import mmmfrieddough.craftpilot.service.CraftPilotService;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(World.class)
public abstract class WorldMixin {
    private CraftPilotService craftPilotService;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        craftPilotService = CraftPilotClient.getInstance().getCraftPilotService();
    }

    @Shadow
    public abstract boolean isClient();

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
    private void onSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        // This should only run on the client
        if (!isClient()) {
            return;
        }

        // Check if a block was actually placed
        if (cir.getReturnValue()) {
            craftPilotService.onBlockPlaced(pos);
        }
    }
}
