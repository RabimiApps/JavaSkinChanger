package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class SkinChangeScreen extends Screen {

    private static final int MAX_WIDTH = 450;
    private static final int MAX_HEIGHT = 275;

    private int windowWidth;
    private int windowHeight;
    private int leftX;
    private int topY;

    private final MinecraftClient client = MinecraftClient.getInstance();

    public SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        // ウィンドウサイズを画面に合わせて調整
        windowWidth = Math.min((int)(this.width * 0.6), MAX_WIDTH);
        windowHeight = Math.min((int)(this.height * 0.4), MAX_HEIGHT);

        leftX = (this.width - windowWidth) / 2;
        topY  = (this.height - windowHeight) / 2;

        int buttonWidth = 120;
        int buttonHeight = 25;
        int padding = 10;

        // 左側・水色 画像選択ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("画像選択"), btn -> {
            System.out.println("画像選択ボタン押下");
            // ここで将来的にスキン変更処理を呼べます
        }).dimensions(leftX + padding, topY + padding + 20, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景（半透明黒）
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);

        // 左上タイトル
        context.drawText(this.textRenderer, Text.of("JavaSkinChanger"), leftX + 5, topY + 5, 0xFFFFFF, false);

        // 中央縦線（白っぽいグレー）
        int centerX = leftX + windowWidth / 2;
        context.fill(centerX - 1, topY, centerX + 1, topY + windowHeight, 0xFFD3D3D3);

        // 左側に3Dプレイヤーモデル表示
        drawPlayerModel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPlayerModel(DrawContext context, int mouseX, int mouseY) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // 表示位置（左側中央）
        int modelX = leftX + windowWidth / 4;
        int modelY = topY + windowHeight / 2 + 40;

        // 3Dモデル描画
        InventoryScreen.drawEntity(context, modelX, modelY, 40, (float)(modelX - mouseX), (float)(modelY - mouseY), player);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
