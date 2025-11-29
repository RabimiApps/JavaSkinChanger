package com.rabimi.javaskinchanger;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {
    private final Minecraft client = Minecraft.getInstance();
    private ResourceLocation customSkin;
    private final ArrayList<File> localSkins = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    private float rotationYaw = 0;
    private float rotationPitch = 0;
    private float zoom = 1.0f;

    private double lastMouseX, lastMouseY;
    private boolean rotating = false;
    private boolean scrolling = false;

    private boolean useAlex = false; // Steve/Alex切替

    protected SkinChangeScreen() {
        super(Component.literal("JavaSkinChanger"));
    }

    @Override
    protected void init() {
        loadLocalSkins();

        // アップロードボタン
        addRenderableWidget(new Button(10, 10, 150, 20, Component.literal("Upload Skin"), b -> openSkinFile()));

        // Mojangスキン取得ボタン
        addRenderableWidget(new Button(170, 10, 150, 20, Component.literal("Fetch Mojang Skin"), b -> fetchMojangSkin()));

        // Steve/Alex切替トグル
        addRenderableWidget(new Button(330, 10, 100, 20, Component.literal("Steve/Alex"), b -> {
            useAlex = !useAlex;
        }));
    }

    private void loadLocalSkins() {
        File dir = new File("config/skins");
        if (!dir.exists()) dir.mkdirs();

        File[] skins = dir.listFiles(f -> f.getName().endsWith(".png"));
        if (skins != null) {
            for (File f : skins) localSkins.add(f);
        }
    }

    private void openSkinFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Minecraft Skin PNG");
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.exists()) return;

        applySkin(file);
    }

    private void applySkin(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            resized.getGraphics().drawImage(img, 0, 0, 64, 64, null);

            NativeImage nativeImage = NativeImage.read(toInputStream(resized));
            customSkin = new ResourceLocation("javaskinchanger", "customskin");
            client.getTextureManager().register(customSkin, new net.minecraft.client.renderer.texture.NativeImageTexture(nativeImage));

            applySkinToPlayer(client.player, customSkin);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchMojangSkin() {
        if (client.player == null) return;

        new Thread(() -> {
            try {
                String uuid = client.player.getGameProfile().getId().toString().replace("-", "");
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                String base64 = com.google.gson.JsonParser.parseString(sb.toString())
                        .getAsJsonObject()
                        .getAsJsonArray("properties")
                        .get(0).getAsJsonObject().get("value").getAsString();

                String decoded = new String(Base64.getDecoder().decode(base64));
                String skinUrl = com.google.gson.JsonParser.parseString(decoded)
                        .getAsJsonObject()
                        .getAsJsonObject("textures")
                        .getAsJsonObject("SKIN")
                        .get("url").getAsString();

                BufferedImage img = ImageIO.read(new URL(skinUrl));
                NativeImage nativeImage = NativeImage.read(toInputStream(img));
                customSkin = new ResourceLocation("javaskinchanger", "mojangskin");

                client.execute(() -> {
                    client.getTextureManager().register(customSkin, new net.minecraft.client.renderer.texture.NativeImageTexture(nativeImage));
                    applySkinToPlayer(client.player, customSkin);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void applySkinToPlayer(@Nullable LocalPlayer player, ResourceLocation skin) {
        if (player == null) return;
        // Minecraft 1.21.x でのカスタムスキン適用はMixinで player.getSkinTexture() を差し替え
    }

    private InputStream toInputStream(BufferedImage img) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // 横スクロール2Dサムネイル
        int startX = 10 - scrollOffset;
        int y = 50;
        int size = 32;
        int padding = 5;

        TextureManager tm = client.getTextureManager();

        for (int i = 0; i < localSkins.size(); i++) {
            File f = localSkins.get(i);
            try {
                BufferedImage img = ImageIO.read(f);
                NativeImage ni = NativeImage.read(toInputStream(img));
                ResourceLocation tex = new ResourceLocation("javaskinchanger", "thumb" + i);
                tm.register(tex, new net.minecraft.client.renderer.texture.NativeImageTexture(ni));

                blit(matrices, startX + i * (size + padding), y, 0, 0, size, size, size, size);

                if (i == selectedIndex) fill(matrices, startX + i * (size + padding) - 2, y - 2,
                        startX + i * (size + padding) + size + 2, y + size + 2, 0xFFFFFFFF);

                if (mouseX >= startX + i * (size + padding) && mouseX <= startX + i * (size + padding) + size &&
                        mouseY >= y && mouseY <= y + size && client.mouseHandler.isLeftPressed()) {

                    selectedIndex = i;
                    applySkin(f);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3DプレイヤープレビューはMixinでカスタムレンダー適用
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rotating) {
            rotationYaw += deltaX * 0.5f;
            rotationPitch -= deltaY * 0.5f;
        } else if (scrolling) {
            scrollOffset -= deltaX;
            if (scrollOffset < 0) scrollOffset = 0;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rotating = false;
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) rotating = true;
        if (button == 1) scrolling = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        zoom += delta * 0.1f;
        if (zoom < 0.1f) zoom = 0.1f;
        if (zoom > 5.0f) zoom = 5.0f;
        return true;
    }
}