package com.rabimi.javaskinchanger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private NativeImageBackedTexture customSkinTexture;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
    }

    @Override
    public void init() {
        // ファイル選択ボタン
        this.addDrawableChild(new ButtonWidget(10, 10, 150, 20, Text.of("Upload Skin"), button -> openSkinFile()));
    }

    private void openSkinFile() {
        // Java Swingファイル選択ダイアログ
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            BufferedImage buffered = ImageIO.read(file);
            // 64x64にリサイズ
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            resized.getGraphics().drawImage(buffered, 0, 0, 64, 64, null);

            NativeImage image = NativeImage.fromBufferedImage(resized);
            Supplier<String> supplier = () -> "javaskinchanger/customskin";
            customSkinTexture = new NativeImageBackedTexture(supplier, image);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
        this.renderBackground(matrices, 0, 0, tickDelta);
        super.render(matrices, mouseX, mouseY, tickDelta);
        renderPlayer3D(matrices, mouseX, mouseY, tickDelta);
    }

    private void renderPlayer3D(MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
        if(client.player == null) return;

        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.player;
        NativeImageBackedTexture texture = customSkinTexture;

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PlayerEntityRenderer renderer = (PlayerEntityRenderer) dispatcher.getRenderer(player);

        matrices.push();
        matrices.translate(200, 150, 1050);
        matrices.scale(-30f, 30f, 30f);

        float yaw = (float)Math.atan((200 - mouseX) / 40.0) * 20;
        float pitch = (float)Math.atan((150 - mouseY) / 40.0) * 20;

        if(texture != null) {
            client.getTextureManager().registerTexture(new net.minecraft.util.Identifier("javaskinchanger/customskin"), texture);
            player.setSkinTexture(new net.minecraft.util.Identifier("javaskinchanger/customskin"));
        }

        renderer.render(player, yaw, pitch, matrices, client.getBufferBuilders().getEntityVertexConsumers(), tickDelta);
        matrices.pop();
    }
}
