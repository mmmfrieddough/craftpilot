package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mmmfrieddough.craftpilot.service.AlphaRenderCommandQueue;
import mmmfrieddough.craftpilot.service.GhostBlockRenderService;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(BlockEntityRenderManager.class)
public class BlockEntityRenderManagerMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;render(Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"))
    @SuppressWarnings("rawtypes")
    private void wrapGhostBlockEntityRender(BlockEntityRenderer renderer,
            BlockEntityRenderState renderState, MatrixStack matrices, OrderedRenderCommandQueue queue,
            CameraRenderState cameraState, Operation<Void> original) {
        if (GhostBlockRenderService.isGhostBlockEntity(renderState.pos)) {
            original.call(renderer, renderState, matrices,
                    new AlphaRenderCommandQueue(queue, GhostBlockRenderService.getActiveOpacity()),
                    cameraState);
        } else {
            original.call(renderer, renderState, matrices, queue, cameraState);
        }
    }
}
