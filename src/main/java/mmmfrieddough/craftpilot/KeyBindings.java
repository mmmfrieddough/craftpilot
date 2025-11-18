package mmmfrieddough.craftpilot;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

public class KeyBindings {
    private static final KeyBinding.Category CRAFTPILOT_CATEGORY = KeyBinding.Category
            .create(Identifier.of(Reference.MOD_ID, "keybindings"));

    private static KeyBinding clearKeyBinding;
    private static KeyBinding triggerKeyBinding;
    private static KeyBinding acceptAllKeyBinding;
    private static KeyBinding selectAlternativeKeyBinding;

    public static void register() {
        KeyBinding clearKey = new KeyBinding("key.craftpilot.clear", GLFW.GLFW_KEY_Z, CRAFTPILOT_CATEGORY);
        clearKeyBinding = KeyBindingHelper.registerKeyBinding(clearKey);

        KeyBinding triggerKey = new KeyBinding("key.craftpilot.trigger", GLFW.GLFW_KEY_R, CRAFTPILOT_CATEGORY);
        triggerKeyBinding = KeyBindingHelper.registerKeyBinding(triggerKey);

        KeyBinding acceptAllKey = new KeyBinding("key.craftpilot.accept_all", GLFW.GLFW_KEY_V, CRAFTPILOT_CATEGORY);
        acceptAllKeyBinding = KeyBindingHelper.registerKeyBinding(acceptAllKey);

        KeyBinding selectAlternativeKey = new KeyBinding("key.craftpilot.select_alternative", GLFW.GLFW_KEY_LEFT_ALT,
                CRAFTPILOT_CATEGORY);
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
