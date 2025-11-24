package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.player.AbstractClientPlayerEntity;
import com.mojang.math.Vector3f;
import com.mojang.math.Quaternion;

public class SkinChangeScreen extends Screen {

    private static final int MAX_WIDTH = 450;
    private static final int MAX_HEIGHT = 275;

    private int windowWidth;
    private int windowHeight;
    private int leftX;
    private int topY;

    private Identifier currentSkinTexture; // 選択中スキン
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

        int buttonWidth = 120;
        int buttonHeight = 25;
        int padding = 10;

        // 左側の画像選択ボタン（水色）
        this.addDrawableChild(ButtonWidget.builder(Text.of("画像選択"), btn -> {
            selectSkinFromAndroid();
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

        // 3Dプレイヤーモデル描画（左側）
        drawPlayerModel(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPlayerModel(DrawContext context, int mouseX, int mouseY) {
        AbstractClientPlayerEntity player = client.player;
        if(player == null) return;

        int modelX = leftX + windowWidth / 4;
        int modelY = topY + windowHeight / 2 + 40;

        float yaw = (float)(modelX - mouseX);
        float pitch = (float)(modelY - mouseY);

        InventoryScreen.drawEntity(
                context,
                modelX,
                modelY,
                40, // スケール
                yaw,
                pitch,
                player
        );
    }

    private void selectSkinFromAndroid() {
        // Android からスキンを受け取り currentSkinTexture にセットする
        System.out.println("Androidからスキン取得処理呼ばれた");
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
