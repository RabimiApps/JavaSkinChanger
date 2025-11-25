package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.Identifier;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;
    private NativeImageBackedTexture customSkinTexture;
    private Identifier customSkinId;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {
        // スキンアップロードボタン
        this.addDrawableChild(new ButtonWidget(
                10, 10, 150, 20,
                Text.of("Upload Skin"),
                button -> openSkinFile(),
                button -> Text.of("Upload a custom skin") // NarrationSupplier
        ));
    }

    private void openSkinFile() {
        // SwingのJFileChooserでファイル選択
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Minecraft Skin PNG");
        int result = chooser.showOpenDialog(null);

        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.exists()) return;

        try {
            BufferedImage buffered = ImageIO.read(file);

            // 64x64にリサイズ
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            resized.getGraphics().drawImage(buffered, 0, 0, 64, 64, null);

            // NativeImage に変換
            NativeImage image = NativeImage.fromBufferedImage(resized);

            // テクスチャ作成
            customSkinTexture = new NativeImageBackedTexture((Supplier<String>) () -> "customskin", image);
            customSkinId = new Identifier("javaskinchanger", "customskin");

            client.getTextureManager().registerTexture(customSkinId, customSkinTexture);

            // プレイヤーに適用
            if (client.player instanceof AbstractClientPlayerEntity player) {
                player.setSkinTexture(customSkinId);
            }

            System.out.println("[JavaSkinChanger] Skin uploaded!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground();
        super.render(matrices, mouseX, mouseY, delta);

        if (client.player != null) {
            // 3Dプレイヤーモデル描画
            if (client.player.getSkinRenderer() instanceof PlayerEntityRenderer renderer) {
                renderer.render(client.player, 0f, 0f, matrices, client.getBufferBuilders().getEntityVertexConsumers(), delta);
            }
        }
    }
}
