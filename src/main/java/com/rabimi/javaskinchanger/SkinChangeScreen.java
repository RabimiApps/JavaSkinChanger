package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class SkinChangeScreen extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
    }

    @Override
    protected void init() {
        int leftX = 10;
        int leftY = 40;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 5;

        // Android風 左側ボタン
        addDrawableChild(new ButtonWidget(leftX, leftY, buttonWidth, buttonHeight, Text.of("モデル変更"), button -> {
            // TODO: モデル切替処理
        }));
        addDrawableChild(new ButtonWidget(leftX, leftY + buttonHeight + spacing, buttonWidth, buttonHeight, Text.of("スキン変更"), button -> {
            // TODO: スキン変更処理
        }));
        addDrawableChild(new ButtonWidget(leftX, leftY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, Text.of("リロード"), button -> {
            // TODO: リロード処理
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // 背景（透明75%）
        fill(matrices, width / 2, 0, width, height, 0xBF000000); // 0xBF = 75% alpha

        // 左上タイトル
        drawTextWithShadow(matrices, textRenderer, "JavaSkinChanger", 5, 5, 0xFFFFFF);

        // 左側の3Dスキン描画（簡易）
        if (mc.player != null) {
            renderEntityInInventory(20, height / 2, 30, mc.player);
        }

        // ボタンや他UI描画
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderEntityInInventory(int x, int y, int scale, AbstractClientPlayerEntity player) {
        // Minecraft 内部の方法で3Dモデル描画
        // 実際の回転やマウス操作はさらに処理が必要
        // このサンプルでは簡易的な呼び出し
        // SkinViewAndroidのような回転可能モデルはここに統合
    }
}