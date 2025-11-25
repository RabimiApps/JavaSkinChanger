package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderHelper;
import net.minecraft.text.Text;

public class SkinChangeScreen extends Screen {

    public SkinChangeScreen() {
        super(Text.literal("Skin Changer"));
    }

    @Override
    protected void init() {

        // スキン選択
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("スキン選択"), b -> {
                    // TODO: ファイル選択画面
                }).dimensions(this.width / 2 - 60, this.height / 2 - 20, 120, 20).build()
        );

        // 適用ボタン
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("適用"), b -> {
                    applySkin();
                }).dimensions(this.width / 2 - 60, this.height / 2 + 10, 120, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 正しい renderBackground
        this.renderBackground(context, mouseX, mouseY, delta);

        drawPlayer3D(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPlayer3D(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int x = this.width / 2;
        int y = this.height / 2 - 60;

        // 1.21.5 正式API
        EntityRenderHelper.renderEntity(
                context,
                x, y, // 表示位置
                40,   // スケール
                0f,   // マウスXオフセット（回転に使える）
                0f,   // マウスYオフセット
                player
        );
    }

    private void applySkin() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.literal("スキン変更しました！"), false);
    }
}
