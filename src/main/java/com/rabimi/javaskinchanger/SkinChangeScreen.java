package com.rabimi.javaskinchanger;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.Quaternionf;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.player.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private static final int MAX_WIDTH = 450;
    private static final int MAX_HEIGHT = 275;

    private int windowWidth;
    private int windowHeight;
    private int leftX;
    private int topY;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private Identifier customSkin = null;

    protected SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        windowWidth = Math.min((int) (this.width * 0.6), MAX_WIDTH);
        windowHeight = Math.min((int) (this.height * 0.4), MAX_HEIGHT);

        leftX = (this.width - windowWidth) / 2;
        topY = (this.height - windowHeight) / 2;

        int buttonWidth = 80;
        int buttonHeight = 20;
        int padding = 10;

        // Change Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Change Skin"), button -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (FileInputStream fis = new FileInputStream(file)) {
                    NativeImage img = NativeImage.read(fis);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "custom_skin", img);
                    customSkin = client.getTextureManager().registerTexture(
                            new Identifier("javaskinchanger", "custom_skin"), tex
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).dimensions(leftX + padding, topY + padding + 30, buttonWidth, buttonHeight).build());

        // Reset Skin ボタン
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> {
            customSkin = null; // 元のスキンに戻す
        }).dimensions(leftX + padding + buttonWidth + padding, topY + padding + 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);

        // タイトル
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);

        // プレイヤー3Dモデル表示
        if (client.player != null) {
            float modelX = leftX + windowWidth - 60f;
            float modelY = topY + windowHeight - 20f;
            float scale = 50f;

            Vector3f rotationVec = new Vector3f(0, 180, 0);
            Quaternionf rot1 = Quaternionf.IDENTITY;
            Quaternionf rot2 = Quaternionf.IDENTITY;

            InventoryScreen.drawEntity(
                    context,
                    modelX,
                    modelY,
                    scale,
                    rotationVec,
                    rot1,
                    rot2,
                    client.player
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
