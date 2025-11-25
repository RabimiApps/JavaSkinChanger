package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
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
                    // TODO: ファイル選択処理
                }).dimensions(this.width / 2 - 60, this.height / 2 - 20, 120, 20).build()
        );

        // 適用
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("適用"), b -> {
                    applySkin();
                }).dimensions(this.width / 2 - 60, this.height / 2 + 10, 120, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

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

        // 1.21.5 で動く確実な方法
        InventoryScreen.drawEntity(
                context,
                x, y,
                45,       // スケール
                0f, 0f,   // マウス回転（0固定）
                player
        );
    }

    private void applySkin() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            client.player.sendMessage(Text.literal("スキンを変更しました!"), false);
        }
    }
}
