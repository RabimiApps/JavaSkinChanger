package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

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

        // ボタンを匿名クラスで作ることで protected コンストラクタを回避
        this.addDrawableChild(new ButtonWidget(leftX + 10, topY + 20, 80, 20, Text.of("Change Skin"), button -> {
            // ボタン処理
        }) {});

        this.addDrawableChild(new ButtonWidget(leftX + 110, topY + 20, 80, 20, Text.of("Reset Skin"), button -> {
            // ボタン処理
        }) {});
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);
        drawContext.drawText(this.textRenderer, Text.of("JavaSkinChanger"), leftX + 5, topY + 5, 0xFFFFFF, false);

        super.render(drawContext, mouseX, mouseY, delta);
    }
}