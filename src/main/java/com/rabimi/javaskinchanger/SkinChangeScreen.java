package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
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

        windowWidth = Math.min((int)(this.width * 0.6), MAX_WIDTH);
        windowHeight = Math.min((int)(this.height * 0.4), MAX_HEIGHT);

        leftX = (this.width - windowWidth) / 2;
        topY  = (this.height - windowHeight) / 2;

        int buttonWidth = 80;
        int buttonHeight = 20;
        int padding = 10;

        // Change Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Change Skin"), button -> {
            client.player.sendMessage(Text.of("Change Skin押された！"), false);
        }).dimensions(leftX + padding, topY + padding + 30, buttonWidth, buttonHeight).build());

        // Reset Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> {
            client.player.sendMessage(Text.of("Reset Skin押された！"), false);
        }).dimensions(leftX + padding + buttonWidth + padding, topY + padding + 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);

        // タイトル
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);

        // ----------------- ★スキン描画処理★ -----------------
        if (client.player != null) {
            int modelX = leftX + windowWidth - 60;   // 右側に表示
            int modelY = topY + windowHeight - 20;   // 下寄せ
            int modelSize = 50;                       // 大きさ

            InventoryScreen.drawEntity(
                    context,
                    modelX,
                    modelY,
                    modelSize,
                    mouseX - modelX,
                    mouseY - modelY,
                    client.player
            );
        }
        // ----------------- ★ここまで★ -----------------
    }
}
