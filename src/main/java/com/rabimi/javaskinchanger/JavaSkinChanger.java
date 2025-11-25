package com.rabimi.javaskinchanger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class JavaSkinChanger implements ClientModInitializer {

    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();

            // キーバインド登録
            openMenuKey = KeyBindingHelper.registerKeyBinding(
                    new KeyBinding(
                            "key.javaskinchanger.open",
                            InputUtil.Type.KEYSYM,
                            GLFW.GLFW_KEY_J,
                            "category.javaskinchanger"
                    )
            );

            // キー押下時にGUIを開く
            ClientTickEvents.END_CLIENT_TICK.register(c -> {
                if (openMenuKey.wasPressed()) {
                    client.setScreen(new SkinChangeScreen());
                }
            });

            System.out.println("[JavaSkinChanger] Loaded!");
        });
    }
}
