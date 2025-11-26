package com.rabimi.javaskinchanger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;
    private NativeImageBackedTexture customSkinTexture;
    private Identifier customSkinId;

    private final ArrayList<File> localSkins = new ArrayList<>();
    private int selectedSkinIndex = -1;
    private int scrollOffset = 0;

    private float rotationYaw = 0;
    private float rotationPitch = 0;
    private float zoom = 1.0f;

    private double lastMouseX, lastMouseY;
    private boolean rotating = false;
    private boolean scrolling = false;

    protected SkinChangeScreen() {
        super(new LiteralText("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {
        loadLocalSkins();

        this.addDrawableChild(new ButtonWidget(
                10, 10, 150, 20,
                new LiteralText("Upload Skin"),
                button -> openSkinFile(),
                () -> (MutableText) new LiteralText("Upload a custom skin PNG")
        ));

        this.addDrawableChild(new ButtonWidget(
                170, 10, 150, 20,
                new LiteralText("Fetch Mojang Skin"),
                button -> fetchMojangSkin(),
                () -> (MutableText) new LiteralText("Fetch skin from official API")
        ));
    }

    private void openSkinFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Minecraft Skin PNG");
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.exists()) return;

        applySkinFile(file, "customskin");
    }

    private void loadLocalSkins() {
        File dir = new File("config/skins");
        if (!dir.exists()) dir.mkdirs();

        File[] skins = dir.listFiles(f -> f.getName().endsWith(".png"));
        if (skins != null) {
            for (File f : skins) localSkins.add(f);
        }
    }

    private void applySkinFile(File file, String idName) {
        try {
            BufferedImage buffered = ImageIO.read(file);
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            resized.getGraphics().drawImage(buffered, 0, 0, 64, 64, null);

            NativeImage image = NativeImage.read(toInputStream(resized));
            customSkinTexture = new NativeImageBackedTexture(image);
            customSkinId = Identifier.tryParse("javaskinchanger:" + idName);

            client.getTextureManager().registerTexture(customSkinId, customSkinTexture);
            applyTextureToPlayer(client.player, customSkinId);

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

                InputStream is = conn.getInputStream();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                String base64 = json.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
                String decoded = new String(java.util.Base64.getDecoder().decode(base64));
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                String skinUrl = textures.getAsJsonObject("SKIN").get("url").getAsString();

                BufferedImage skinImg = ImageIO.read(new URL(skinUrl));
                NativeImage image = NativeImage.read(toInputStream(skinImg));
                customSkinTexture = new NativeImageBackedTexture(image);
                customSkinId = Identifier.tryParse("javaskinchanger:mojangskin");

                client.execute(() -> {
                    client.getTextureManager().registerTexture(customSkinId, customSkinTexture);
                    applyTextureToPlayer(client.player, customSkinId);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void applyTextureToPlayer(ClientPlayerEntity player, Identifier id) {
        // Mixinなどで player.skinTexture を差し替え
    }

    private InputStream toInputStream(BufferedImage img) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // 横スクロールスキンリスト
        int startX = 10 - scrollOffset;
        int y = 100;
        int thumbSize = 32;
        int padding = 5;

        for (int i = 0; i < localSkins.size(); i++) {
            File f = localSkins.get(i);
            try {
                BufferedImage img = ImageIO.read(f);
                NativeImage texImg = NativeImage.read(toInputStream(img));
                Identifier texId = Identifier.tryParse("javaskinchanger:thumb" + i);
                client.getTextureManager().registerTexture(texId, new NativeImageBackedTexture(texImg));

                context.drawTexture(texId, startX + i * (thumbSize + padding), y, 0, 0, img.getWidth(), img.getHeight(), thumbSize, thumbSize);

                if (i == selectedSkinIndex) {
                    context.fillGradient(startX + i * (thumbSize + padding) - 2, y - 2,
                                         startX + i * (thumbSize + padding) + thumbSize + 2,
                                         y + thumbSize + 2,
                                         0xFFFFFFFF, 0xFFFFFFFF);
                }

                if (mouseX >= startX + i * (thumbSize + padding) && mouseX <= startX + i * (thumbSize + padding) + thumbSize &&
                    mouseY >= y && mouseY <= y + thumbSize &&
                    client.mouse.isLeftPressed()) {

                    selectedSkinIndex = i;
                    applySkinFile(f, "localskin" + i);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // プレイヤー3Dプレビュー
        if (client.player != null) {
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            PlayerEntityRenderer renderer = (PlayerEntityRenderer) dispatcher.getRenderer(client.player);

            context.getMatrices().push();
            context.getMatrices().translate(width / 2.0, height / 2.0, 100.0);
            context.getMatrices().scale(zoom, zoom, zoom);
            context.getMatrices().multiply(net.minecraft.util.math.Vec3f.POSITIVE_Y.getDegreesQuaternion(rotationYaw));
            context.getMatrices().multiply(net.minecraft.util.math.Vec3f.POSITIVE_X.getDegreesQuaternion(rotationPitch));

            renderer.render(client.player, 0f, 0f, context.getMatrices(), client.getBufferBuilders().getEntityVertexConsumers(), 0xF000F0, delta);
            context.getMatrices().pop();
        }
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
        if (button == 0) rotating = true;    // 左クリックで回転
        if (button == 1) scrolling = true;   // 右クリックでスクロール
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        zoom += amount * 0.1f;
        if (zoom < 0.1f) zoom = 0.1f;
        if (zoom > 5.0f) zoom = 5.0f;
        return true;
    }
}