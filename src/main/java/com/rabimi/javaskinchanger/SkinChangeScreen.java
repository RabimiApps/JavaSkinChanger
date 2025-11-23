package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class SkinChangeScreen extends Screen {

    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 100;

    private int leftX;
    private int topY;

    public SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        // 画面中央にウィンドウ配置
        leftX = (this.width - WINDOW_WIDTH) / 2;
        topY = (this.height - WINDOW_HEIGHT) / 2;

        // ボタン追加
        this.addDrawableChild(new ButtonWidget(leftX + 10, topY + 40, 80, 20, Text.of("Change Skin"), button -> {
            // スキン変更処理
        }));

        this.addDrawableChild(new ButtonWidget(leftX + 110, topY + 40, 80, 20, Text.of("Reset Skin"), button -> {
            // スキンリセット処理
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景半透明
        context.fill(leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);

        // タイトル描画
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }
}