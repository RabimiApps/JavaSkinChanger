package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;

public class SkinChangeScreen extends Screen {

    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 100;
    private int leftX;
    private int topY;

    protected SkinChangeScreen() {
        super(Text.of("SkinChanger"));
    }

    @Override
    protected void init() {
        leftX = (this.width - WINDOW_WIDTH) / 2;
        topY = (this.height - WINDOW_HEIGHT) / 2;

        this.addDrawableChild(new ButtonWidget(
            leftX + 10, topY + 20, 80, 20,
            Text.of("Change Skin"),
            button -> {
                // ボタン押下時の処理
            },
            ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));

        this.addDrawableChild(new ButtonWidget(
            leftX + 110, topY + 20, 80, 20,
            Text.of("Reset Skin"),
            button -> {
                // ボタン押下時の処理
            },
            ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // 背景
        drawContext.fill(leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);

        // タイトル
        drawContext.drawText(this.textRenderer, Text.of("JavaSkinChanger"), leftX + 5, topY + 5, 0xFFFFFF, false);

        super.render(drawContext, mouseX, mouseY, delta);
    }
}