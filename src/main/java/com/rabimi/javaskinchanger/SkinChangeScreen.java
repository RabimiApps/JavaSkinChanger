package com.rabimi.javaskinchanger;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;
    private NativeImageBackedTexture customSkinTexture;
    private Identifier customSkinId;
    private Map<String, Identifier> skinLibrary = new HashMap<>();
    private float rotation = 0f;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
        loadSkinLibrary();
    }

    @Override
    protected void init() {
        super.init();

        // アップロードボタン
        this.addDrawableChild(new ButtonWidget(
                10, 10, 150, 20,
                Text.of("Upload Skin"),
                button -> openSkinFile(),
                () -> Text.of("Upload a custom skin PNG")
        ));

        // ライブラリボタン（サンプル1個）
        this.addDrawableChild(new ButtonWidget(
                10, 40, 150, 20,
                Text.of("Load from Library"),
                button -> applyLibrarySkin("default"),
                () -> Text.of("Apply saved skin")
        ));
    }

    private void loadSkinLibrary() {
        // config/javaskinchanger/skins.json を読み込む
        File dir = new File("config/javaskinchanger");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "skins.json");
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                // key:value → keyは名前、valueはPNGパス
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    File f = new File(dir, parts[1]);
                    if (f.exists()) {
                        NativeImage img = NativeImage.fromFile(f);
                        Identifier id = Identifier.tryParse("javaskinchanger:" + parts[0]);
                        client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
                        skinLibrary.put(parts[0], id);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyLibrarySkin(String name) {
        Identifier id = skinLibrary.get(name);
        if (id != null && client.player instanceof AbstractClientPlayerEntity player) {
            try {
                player.getClass().getMethod("setSkinTexture", Identifier.class).invoke(player, id);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    private void openSkinFile() {
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

            NativeImage image = NativeImage.fromBufferedImage(resized);
            customSkinTexture = new NativeImageBackedTexture((Supplier<String>) () -> "customskin", image);
            customSkinId = Identifier.tryParse("javaskinchanger:customskin");
            client.getTextureManager().registerTexture(customSkinId, customSkinTexture);

            if (client.player instanceof AbstractClientPlayerEntity player) {
                player.getClass().getMethod("setSkinTexture", Identifier.class).invoke(player, customSkinId);
            }

        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private void fetchOfficialSkin() {
        if (client.player == null) return;
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.player;

        // MojangスキンAPI
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/"
                    + player.getUuid().toString().replace("-", "") + "?unsigned=false");
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                Identifier id = Identifier.tryParse("javaskinchanger:official");
                NativeImage img = NativeImage.fromInputStream(new ByteArrayInputStream(bytes));
                client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));

                player.getClass().getMethod("setSkinTexture", Identifier.class).invoke(player, id);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        if (client.player != null) {
            // 簡易3Dプレビュー
            rotation += delta * 5f;
            if (client.player.getSkinRenderer() instanceof PlayerEntityRenderer renderer) {
                renderer.render(client.player, rotation, 0f, matrices, client.getBufferBuilders().getEntityVertexConsumers(), delta);
            }
        }
    }
}