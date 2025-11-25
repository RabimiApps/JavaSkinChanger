package com.rabimi.javaskinchanger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;
    private float modelYaw = 180f;
    private float modelPitch = 0f;
    private boolean dragging = false;

    private NativeImageBackedTexture customSkinTexture = null;
    private File selectedImage = null;

    public SkinChangeScreen() {
        super(Text.literal("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {

        int cx = this.width / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("画像を選択"), (button) -> {
            selectImage();
        }).dimensions(cx - 60, 40, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("スキン適用"), (button) -> {
            applySkin();
        }).dimensions(cx - 60, 70, 120, 20).build());
    }

    /** OSのファイルピッカーを開く（実質JavaFX無しでの簡易版） */
    private void selectImage() {
        try {
            File dialog = new File("skin.png"); // GitHub Actions でも扱いやすい仮ファイル
            if (dialog.exists()) {
                selectedImage = dialog;
                client.player.sendMessage(Text.literal("画像を選択しました: skin.png"), false);
            } else {
                client.player.sendMessage(Text.literal("skin.png を置いてください。"), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Mojang APIに送らず、クライアント側に直接読み込む */
    private void applySkin() {
        if (selectedImage == null) {
            client.player.sendMessage(Text.literal("画像が選択されていません"), false);
            return;
        }

        try {
            NativeImage image = NativeImage.read(Files.newInputStream(selectedImage.toPath()));
            if (customSkinTexture != null) {
                customSkinTexture.close();
            }

            customSkinTexture = new NativeImageBackedTexture(image);
            client.getTextureManager().registerTexture(
                    client.player.getSkinTexture(),
                    customSkinTexture
            );

            client.player.sendMessage(Text.literal("スキンを変更しました！"), false);

        } catch (IOException e) {
            client.player.sendMessage(Text.literal("読み込み失敗"), false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            modelYaw += dx * 2f;
            modelPitch += dy * 2f;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int x = this.width / 2;
        int y = this.height - 20;

        Quaternionf bodyRot = new Quaternionf().rotationY((float) Math.toRadians(modelYaw));
        Quaternionf headRot = new Quaternionf().rotationX((float) Math.toRadians(modelPitch));

        // === 正しい 1.21.5 の描画メソッド ===
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                context,
                (float) x,
                (float) y,
                50f,
                new Vector3f(0f, 0f, 0f),
                headRot,
                bodyRot,
                player
        );
    }
}
