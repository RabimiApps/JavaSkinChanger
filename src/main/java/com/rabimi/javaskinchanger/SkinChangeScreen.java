package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.Identifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private MinecraftClient client;
    private AbstractClientPlayerEntity player;
    private Identifier customSkinId;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
        this.player = client.player;
        this.customSkinId = null;
    }

    @Override
    protected void init() {
        int y = 20;

        this.addDrawableChild(new ButtonWidget(
                10, y, 150, 20,
                Text.of("Upload Skin"),
                button -> openSkinFile(),
                () -> Text.of("Upload a custom skin PNG")
        ));
    }

    private void openSkinFile() {
        try {
            File file = new File(System.getProperty("user.home") + "/skin.png");
            if (!file.exists()) return;

            BufferedImage original = ImageIO.read(file);
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, 64, 64, null);
            g.dispose();

            // 保存してNativeImageに読み込む
            File tmpFile = new File(System.getProperty("java.io.tmpdir"), "tmp_skin.png");
            ImageIO.write(resized, "PNG", tmpFile);
            customSkinId = new Identifier("javaskinchanger", "customskin");
            client.getTextureManager().registerTexture(customSkinId, net.minecraft.client.texture.NativeImageBackedTexture.read(tmpFile));
            
            // プレイヤーに適用
            player.setSkinTexture(customSkinId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        super.render(matrices, mouseX, mouseY, delta);

        // 3Dプレイヤー表示
        int centerX = this.width / 2;
        int centerY = this.height / 2 + 20;

        float yaw = (float) (Math.sin(System.currentTimeMillis() * 0.002) * 30);
        float pitch = 0;

        LivingEntityRenderer<AbstractClientPlayerEntity, ?> renderer = 
                (LivingEntityRenderer<AbstractClientPlayerEntity, ?>) client.getEntityRenderDispatcher().getRenderer(player);
        renderer.render(
                player,
                yaw,
                pitch,
                matrices,
                client.getBufferBuilders().getEntityVertexConsumers(),
                delta
        );
    }
}
