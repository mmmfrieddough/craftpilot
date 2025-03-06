package mmmfrieddough.craftpilot;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding clearKeyBinding;
    private static KeyBinding triggerKeyBinding;

    public static void register() {
        KeyBinding clearKey = new KeyBinding("key.craftpilot.clear", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C,
                "key.categories.craftpilot");
        clearKeyBinding = KeyBindingHelper.registerKeyBinding(clearKey);

        KeyBinding triggerKey = new KeyBinding("key.craftpilot.trigger", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R,
                "key.categories.craftpilot");
        triggerKeyBinding = KeyBindingHelper.registerKeyBinding(triggerKey);
    }

    public static KeyBinding getClearKeyBinding() {
        return clearKeyBinding;
    }

    public static KeyBinding getTriggerKeyBinding() {
        return triggerKeyBinding;
    }
}
