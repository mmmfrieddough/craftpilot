package mmmfrieddough.craftpilot;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding clearKeyBinding;
    private static KeyBinding triggerKeyBinding;
    private static KeyBinding acceptAllKeyBinding;
    private static KeyBinding selectAlternativeKeyBinding;

    public static void register() {
        KeyBinding clearKey = new KeyBinding("key.craftpilot.clear", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z,
                "key.categories.craftpilot");
        clearKeyBinding = KeyBindingHelper.registerKeyBinding(clearKey);

        KeyBinding triggerKey = new KeyBinding("key.craftpilot.trigger", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R,
                "key.categories.craftpilot");
        triggerKeyBinding = KeyBindingHelper.registerKeyBinding(triggerKey);

        KeyBinding acceptAllKey = new KeyBinding("key.craftpilot.accept_all", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V,
                "key.categories.craftpilot");
        acceptAllKeyBinding = KeyBindingHelper.registerKeyBinding(acceptAllKey);

        KeyBinding selectAlternativeKey = new KeyBinding("key.craftpilot.select_alternative", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT, "key.categories.craftpilot");
        selectAlternativeKeyBinding = KeyBindingHelper.registerKeyBinding(selectAlternativeKey);
    }

    public static KeyBinding getClearKeyBinding() {
        return clearKeyBinding;
    }

    public static KeyBinding getTriggerKeyBinding() {
        return triggerKeyBinding;
    }

    public static KeyBinding getAcceptAllKeyBinding() {
        return acceptAllKeyBinding;
    }

    public static KeyBinding getSelectAlternativeKeyBinding() {
        return selectAlternativeKeyBinding;
    }
}
