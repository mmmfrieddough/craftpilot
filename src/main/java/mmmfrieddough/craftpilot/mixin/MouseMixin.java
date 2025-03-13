package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.KeyBindings;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MouseMixin {
    private IWorldManager worldManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        worldManager = CraftPilot.getInstance().getWorldManager();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (KeyBindings.getSelectAlternativeKeyBinding().isPressed()) {
            worldManager.advanceSelectedAlternativeNum(-1 * (int) vertical);
            ci.cancel();
        }
    }
}
