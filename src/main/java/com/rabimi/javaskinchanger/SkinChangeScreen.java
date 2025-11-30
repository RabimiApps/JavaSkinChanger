package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
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
public class SkinChangeScreen2D extends Screen {
    private final Minecraft client = Minecraft.getInstance();
    private final ArrayList<File> localSkins = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    private boolean useAlex = false; // Steve/Alex切替

    private static ResourceLocation currentCustomSkin;

    protected SkinChangeScreen2D() {
        super(Component.literal("JavaSkinChanger 2D"));
    }

    @Override
    protected void init() {
        loadLocalSkins();

        addRenderableWidget(new Button(10, 10, 150, 20, Component.literal("Upload Skin"), b -> openSkinFile()));
        addRenderableWidget(new Button(170, 10, 150, 20, Component.literal("Fetch Mojang Skin"), b -> fetchMojangSkin()));
        addRenderableWidget(new Button(330, 10, 100, 20, Component.literal("Steve/Alex"), b -> useAlex = !useAlex));
    }

    private void loadLocalSkins() {
        File dir = new File("config/skins");
        if (!dir.exists()) dir.mkdirs();
        File[] skins = dir.listFiles(f -> f.getName().endsWith(".png"));
        if (skins != null) for (File f : skins) localSkins.add(f);
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
            NativeImage nativeImage = NativeImage.read(toInputStream(img));
            ResourceLocation skin = new ResourceLocation("javaskinchanger", "customskin");
            client.getTextureManager().register(skin, new net.minecraft.client.renderer.texture.NativeImageTexture(nativeImage));
            currentCustomSkin = skin;
            applySkinToPlayer(client.player, skin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ResourceLocation getCustomSkin() {
        return currentCustomSkin;
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
                ResourceLocation skin = new ResourceLocation("javaskinchanger", "mojangskin");

                client.execute(() -> {
                    client.getTextureManager().register(skin, new net.minecraft.client.renderer.texture.NativeImageTexture(nativeImage));
                    currentCustomSkin = skin;
                    applySkinToPlayer(client.player, skin);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void applySkinToPlayer(@Nullable LocalPlayer player, ResourceLocation skin) {
        if (player == null) return;
        // ここはMixinで差し替え対応
    }

    private InputStream toInputStream(BufferedImage img) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        // 2Dサムネイル表示
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

        super.render(matrices, mouseX, mouseY, delta);
    }
}