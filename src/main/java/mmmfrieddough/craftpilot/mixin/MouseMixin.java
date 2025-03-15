package mmmfrieddough.craftpilot.mixin;

import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.KeyBindings;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.Scroller;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    private MinecraftClient client;
    @Shadow
    private Scroller scroller;

    private IWorldManager worldManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        worldManager = CraftPilot.getInstance().getWorldManager();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (KeyBindings.getSelectAlternativeKeyBinding().isPressed()) {
            if (worldManager.getTotalAlternativeNum() == 0) {
                return;
            }

            boolean discreteScrollEnabled = this.client.options.getDiscreteMouseScroll().getValue();
            double mouseWheelSensitivity = this.client.options.getMouseWheelSensitivity().getValue();
            double adjustedHorizontalScroll = (discreteScrollEnabled ? Math.signum(horizontal) : horizontal)
                    * mouseWheelSensitivity;
            double adjustedVerticalScroll = (discreteScrollEnabled ? Math.signum(vertical) : vertical)
                    * mouseWheelSensitivity;
            Vector2i scrollVector = this.scroller.update(adjustedHorizontalScroll, adjustedVerticalScroll);
            int scrollAmount = scrollVector.y == 0 ? -scrollVector.x : scrollVector.y;
            worldManager.setSelectedAlternativeNum(Scroller.scrollCycling((double) scrollAmount,
                    worldManager.getSelectedAlternativeNum(), worldManager.getTotalAlternativeNum()));
            ci.cancel();
        }
    }
}