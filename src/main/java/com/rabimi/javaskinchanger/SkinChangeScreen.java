package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class SkinChangeScreen extends Screen {

    private int leftX = 10;
    private int leftY = 40;
    private int buttonWidth = 150;
    private int buttonHeight = 20;
    private int spacing = 5;

    // 3Dスキン表示用（簡易）
    private AbstractClientPlayerEntity playerEntity;

    protected SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        // 左側ボタン
        addDrawableChild(new SimpleButton(leftX, leftY, buttonWidth, buttonHeight, Text.of("モデル変更"), button -> {
            // モデル変更処理
        }));

        addDrawableChild(new SimpleButton(leftX, leftY + buttonHeight + spacing, buttonWidth, buttonHeight, Text.of("スキン変更"), button -> {
            // スキン変更処理
        }));

        addDrawableChild(new SimpleButton(leftX, leftY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, Text.of("リロード"), button -> {
            // リロード処理
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // 背景透明75%
        fill(matrices, 0, 0, width, height, 0xBF000000);

        // 左上タイトル
        textRenderer.drawWithShadow(matrices, "JavaSkinChanger", 5, 5, 0xFFFFFF);

        // ここに左側3Dスキン描画処理を入れる（PlayerEntityModelなどを利用）

        super.render(matrices, mouseX, mouseY, delta);
    }

    /** サブクラス化でButtonWidget保護コンストラクタ回避 */
    private static class SimpleButton extends ButtonWidget {
        public SimpleButton(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        }
    }
}