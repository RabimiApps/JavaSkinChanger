package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;
    private Identifier customSkinId;

    protected SkinChangeScreen() {
        super(Text.of("Skin Changer"));
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        // ボタン追加 (NarrationSupplier も必須)
        this.addDrawableChild(new ButtonWidget(
                10, 10, 150, 20,
                Text.of("Upload Skin"),
                button -> openSkinFile(),
                () -> Text.of("Upload a custom skin PNG")
        ));
    }

    // ファイル選択 & 64x64 にリサイズして NativeImage に変換
    private void openSkinFile() {
        // 実際は JFileChooser などでファイルを選択
        File tmpFile = new File("config/custom_skin.png");

        if (!tmpFile.exists()) return;

        try {
            // PNG → NativeImage
            NativeImage nativeImage = NativeImage.fromFile(tmpFile);

            // 64x64にリサイズ
            if (nativeImage.getWidth() != 64 || nativeImage.getHeight() != 64) {
                NativeImage resized = new NativeImage(64, 64, true);
                nativeImage.copyRect(resized, 0, 0, 0, 0, Math.min(nativeImage.getWidth(), 64), Math.min(nativeImage.getHeight(), 64));
                nativeImage.close();
                nativeImage = resized;
            }

            // Texture登録
            customSkinId = new Identifier("javaskinchanger", "customskin");
            client.getTextureManager().registerTexture(customSkinId, new NativeImageBackedTexture(nativeImage));

            // プレイヤーに適用
            if (client.player != null) {
                client.player.setSkinTexture(customSkinId);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices); // 背景描画
        super.render(matrices, mouseX, mouseY, delta);

        // プレイヤー立体描画
        if (client.player != null) {
            // ここは簡易版、EntityRendererに依存せず MatrixStack + RenderSystemで描画
            // 3D描画の詳細はPlayerEntityRendererを直接呼ぶ必要あり
        }
    }
}
