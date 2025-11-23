package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;

public class SkinChangeScreen extends Screen {
    private final int buttonWidth = 150;
    private final int buttonHeight = 20;
    private final int spacing = 5;
    private final int leftX = 5;
    private final int leftY = 30;

    public SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
    }

    @Override
    protected void init() {
        // ボタンを右側に配置
        int rightX = this.width - buttonWidth - 10;
        int topY = 30;

        addDrawableChild(new ButtonWidget(
                rightX, topY, buttonWidth, buttonHeight,
                Text.of("モデル変更"),
                button -> { /* モデル変更処理 */ },
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));

        addDrawableChild(new ButtonWidget(
                rightX, topY + buttonHeight + spacing, buttonWidth, buttonHeight,
                Text.of("スキン変更"),
                button -> { /* スキン変更処理 */ },
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));

        addDrawableChild(new ButtonWidget(
                rightX, topY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight,
                Text.of("リロード"),
                button -> { /* リロード処理 */ },
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景塗りつぶし (75%透明)
        context.fill(0, 0, this.width, this.height, 0xBF000000);

        // 左上タイトル
        context.drawTextWithShadow(this.textRenderer, "JavaSkinChanger", 5, 5, 0xFFFFFF);

        // 左側 3Dスキン描画 (仮)
        // renderSkin3D(context, leftX, leftY, ...);

        super.render(context, mouseX, mouseY, delta);
    }
}