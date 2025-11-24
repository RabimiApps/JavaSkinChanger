package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkinChangeScreen extends Screen {

    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 100;

    private int leftX;
    private int topY;

    // MinecraftClient インスタンス
    private final MinecraftClient client = MinecraftClient.getInstance();

    // デフォルトスキン
    private final Identifier DEFAULT_SKIN = new Identifier("minecraft", "textures/entity/steve.png");

    public SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        leftX = (this.width - WINDOW_WIDTH) / 2;
        topY = (this.height - WINDOW_HEIGHT) / 2;

        // Change Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Change Skin"), button -> {
            // ここでスキンURLを指定してスキン変更
            // 例: https://crafatar.com/avatars/<UUID>
            client.player.setSkin(new Identifier("minecraft", "textures/entity/alex.png"));
        }).dimensions(leftX + 10, topY + 40, 80, 20).build());

        // Reset Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> {
            // デフォルトスキンに戻す
            client.player.setSkin(DEFAULT_SKIN);
        }).dimensions(leftX + 110, topY + 40, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景半透明
        context.fill(leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);

        // タイトル描画
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        // ボタン描画
        super.render(context, mouseX, mouseY, delta);
    }
}
