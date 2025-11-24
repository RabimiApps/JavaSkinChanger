package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Identifier;
import com.mojang.math.Vector3f;
import com.mojang.math.Quaternion;

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

        int buttonWidth = 120;
        int buttonHeight = 25;
        int padding = 10;

        // 左下・水色 画像選択ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("画像選択"), btn -> {
            System.out.println("画像選択ボタン押下");
        }).dimensions(leftX + padding, topY + windowHeight - buttonHeight - padding, buttonWidth, buttonHeight).build());
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
        drawPlayerModel(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPlayerModel(DrawContext context) {
        if (client.player == null) return;

        int modelX = leftX + windowWidth / 4;
        int modelY = topY + windowHeight / 2 + 20;

        // 回転ベクトルとクォータニオン
        Vector3f rotationVec = new Vector3f(0, 180, 0);
        Quaternion rot1 = Quaternion.ONE;
        Quaternion rot2 = Quaternion.ONE;

        InventoryScreen.drawEntity(
            context,
            (float)modelX,
            (float)modelY,
            30f,                 // モデルサイズ
            rotationVec,
            rot1,
            rot2,
            (LivingEntity) client.player
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
