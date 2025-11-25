package com.rabimi.javaskinchanger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class JavaSkinChanger implements ClientModInitializer {

    private static KeyBinding key;

    @Override
    public void onInitializeClient() {

        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.jsc.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.jsc"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (key.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new SkinChangeScreen());
            }
        });
    }
}