package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.util.GhostBlockGlobal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getStackInHand", at = @At("HEAD"), cancellable = true)
    private void getStackInHandOverride(Hand hand, CallbackInfoReturnable<ItemStack> cir) {
        // This should only run on the server
        if (((LivingEntity) (Object) this).getWorld().isClient()) {
            return;
        }

        ItemStack handItem = GhostBlockGlobal.getHandItem();
        if (handItem != null && GhostBlockGlobal.getActiveHand() == hand) {
            cir.setReturnValue(handItem);
        }
    }
}
