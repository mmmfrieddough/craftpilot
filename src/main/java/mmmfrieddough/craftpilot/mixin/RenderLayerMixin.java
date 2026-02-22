package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.service.GhostBlockRenderService;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

@Mixin(RenderLayers.class)
public class RenderLayerMixin {

    @Inject(method = "entitySolid", at = @At("RETURN"))
    private static void onGetEntitySolid(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        GhostBlockRenderService.registerTranslucentLayer(
                cir.getReturnValue(), RenderLayers.entityTranslucent(texture));
    }

    @Inject(method = "entityCutout", at = @At("RETURN"))
    private static void onGetEntityCutout(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        GhostBlockRenderService.registerTranslucentLayer(
                cir.getReturnValue(), RenderLayers.entityTranslucent(texture));
    }

    @Inject(method = "entityCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;", at = @At("RETURN"))
    private static void onGetEntityCutoutNoCull(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        GhostBlockRenderService.registerTranslucentLayer(
                cir.getReturnValue(), RenderLayers.entityTranslucent(texture));
    }
}
