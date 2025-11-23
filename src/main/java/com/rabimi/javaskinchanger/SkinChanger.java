package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.narration.NarrationMessageBuilder;

public class SkinChangeScreen extends Screen {

    protected SkinChangeScreen() {
        super(Text.of("Skin Changer"));
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();

        // NarrationSupplier を簡易で作る
        NarrationSupplier narration = () -> new NarrationMessageBuilder().build();

        // Apply Skin ボタン
        this.addDrawableChild(new ButtonWidget(
                10, 40, 150, 20, Text.of("アップロード"),
                button -> SkinChanger.applySkin("https://example.com/skin.png"),
                narration
        ));

        // Cancel ボタン
        this.addDrawableChild(new ButtonWidget(
                10, 70, 150, 20, Text.of("キャンセル"),
                button -> client.setScreen(null),
                narration
        ));
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}