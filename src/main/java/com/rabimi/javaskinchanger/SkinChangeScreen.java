package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.FileDialog;
import java.awt.Frame;

public class SkinChangeScreen extends Screen {

    private static final int MAX_WIDTH = 450;
    private static final int MAX_HEIGHT = 275;

    private int windowWidth;
    private int windowHeight;
    private int leftX;
    private int topY;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private Identifier customSkin = null;

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

        int buttonWidth = 100;
        int buttonHeight = 20;
        int padding = 10;

        // ファイル選択ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Choose Skin"), button -> {
            selectSkinFile();
        }).dimensions(leftX + padding, topY + padding + 30, buttonWidth, buttonHeight).build());

        // Reset Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> {
            customSkin = null;
            client.player.sendMessage(Text.of("スキンをリセットしました！"), false);
        }).dimensions(leftX + padding + buttonWidth + padding, topY + padding + 30, buttonWidth, buttonHeight).build());
    }

    private void selectSkinFile() {
        new Thread(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "Select Skin PNG", FileDialog.LOAD);
            dialog.setFile("*.png");
            dialog.setVisible(true);
            String directory = dialog.getDirectory();
            String file = dialog.getFile();

            if (directory != null && file != null) {
                File skinFile = new File(directory, file);
                try {
                    NativeImage img = NativeImage.read(new FileInputStream(skinFile));
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                    customSkin = client.getTextureManager().registerDynamicTexture("custom_skin", tex);
                    client.player.sendMessage(Text.of("スキン変更完了！"), false);
                } catch (IOException e) {
                    client.player.sendMessage(Text.of("スキン読み込み失敗！"), false);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);
        // タイトル
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        // プレイヤー3Dモデル表示
        if (client.player != null) {
            ClientPlayerEntity player = client.player;

            if (customSkin != null) {
                player.setSkinTexture(customSkin); // Fabric 1.21.x で動く
            }

            InventoryScreen.drawEntity(
                    context,
                    leftX + windowWidth - 60,
                    topY + windowHeight - 20,
                    50,
                    leftX + windowWidth / 2 - mouseX,
                    topY + windowHeight / 2 - mouseY,
                    player
            );

            if (customSkin != null) {
                player.resetSkinTexture();
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
