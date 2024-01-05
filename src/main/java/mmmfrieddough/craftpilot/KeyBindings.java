package mmmfrieddough.craftpilot;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding exampleKeyBinding;

    public static void register() {
        // GLFW.GLFW_KEY_R is the key here, change it to your desired key
        exampleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mod.example", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // Type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                "category.mod.example" // The translation key of the keybinding's category.
        ));
    }

    public static KeyBinding getExampleKeyBinding() {
        return exampleKeyBinding;
    }
}
