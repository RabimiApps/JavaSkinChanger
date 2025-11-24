package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;

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

        // Change Skin ボタン (PC用ファイル選択)
        this.addDrawableChild(ButtonWidget.builder(Text.of("Change Skin"), button -> {
            SwingUtilities.invokeLater(() -> {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try (FileInputStream fis = new FileInputStream(file)) {
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(fis.readAllBytes());
                        // テクスチャ名は一意にする
                        customSkin = client.getTextureManager().registerDynamicTexture("custom_skin", tex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }).dimensions(leftX + padding, topY + padding + 30, buttonWidth, buttonHeight).build());

        // Reset Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> customSkin = null)
                .dimensions(leftX + padding + buttonWidth + padding, topY + padding + 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        if (customSkin != null) {
            int modelX = leftX + windowWidth - 60;
            int modelY = topY + windowHeight - 20;
            int size = 50;

            AbstractClientPlayerEntity fakePlayer = new ClientPlayerEntity(client.world, client.player.getGameProfile());
            fakePlayer.setSkinTexture(customSkin);

            InventoryScreen.drawEntity(
                    context, modelX, modelY, size, mouseX - modelX, mouseY - modelY, fakePlayer
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
