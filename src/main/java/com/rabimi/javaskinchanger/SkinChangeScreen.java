package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class SkinChangeScreen extends Screen {

    protected SkinChangeScreen() {
        super(Text.of("Skin Changer"));
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Apply Skin ボタン
        this.addDrawableChild(new ButtonWidget(10, 40, 150, 20, Text.of("アップロード"), button -> {
            // ここで SkinChanger を呼ぶ
            SkinChanger.applySkin("https://example.com/skin.png");
        }));

        // Cancel ボタン
        this.addDrawableChild(new ButtonWidget(10, 70, 150, 20, Text.of("キャンセル"), button -> {
            client.setScreen(null); // 前の画面に戻る
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        // 背景描画
        this.renderBackground();
        super.render(mouseX, mouseY, delta);
    }
}