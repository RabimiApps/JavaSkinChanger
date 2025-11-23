package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class SkinChangeScreen extends Screen {

    private static final int WINDOW_WIDTH = 250;
    private static final int WINDOW_HEIGHT = 200;

    public SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
    }

    @Override
    protected void init() {
        super.init();

        // ウィンドウ中央座標
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int leftX = centerX - WINDOW_WIDTH / 2;
        int topY = centerY - WINDOW_HEIGHT / 2;

        int buttonWidth = 80;
        int buttonHeight = 20;
        int spacing = 5;

        // モデル変更ボタン
        this.addDrawableChild(new ButtonWidget(
                leftX + 10,
                topY + 50,
                buttonWidth,
                buttonHeight,
                Text.of("モデル変更"),
                button -> {
                    // TODO: モデル変更処理
                }
        ));

        // スキン変更ボタン
        this.addDrawableChild(new ButtonWidget(
                leftX + 10,
                topY + 50 + buttonHeight + spacing,
                buttonWidth,
                buttonHeight,
                Text.of("スキン変更"),
                button -> {
                    // TODO: スキン変更処理
                }
        ));

        // リロードボタン
        this.addDrawableChild(new ButtonWidget(
                leftX + 10,
                topY + 50 + (buttonHeight + spacing) * 2,
                buttonWidth,
                buttonHeight,
                Text.of("リロード"),
                button -> {
                    // TODO: リロード処理
                }
        ));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // 背景半透明
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - WINDOW_WIDTH / 2;
        int topY = centerY - WINDOW_HEIGHT / 2;
        fill(matrices, leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);

        // 左上テキスト
        this.textRenderer.drawWithShadow(matrices, "JavaSkinChanger", leftX + 5, topY + 5, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);

        // TODO: 左側3Dスキン描画
        // RenderSkinnedModel.render(matrices, leftX + 10, topY + 30, 50, playerSkin);
    }
}