package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SkinChangeScreen
 * - single GUI (Android-like) with:
 *   - local skin library (thumbnails)
 *   - upload button (JFileChooser fallback)
 *   - fetch official Mojang skin
 *   - 3D player preview you can rotate (drag) and zoom (scroll)
 *
 * Notes:
 * - This is a best-effort implementation for Fabric 1.21.x (Yarn mappings).
 * - Some method signatures vary slightly across micro-versions; if you get
 *   compile errors related to render/texture APIs, see the comments below
 *   and tweak to your mapping (Identifier package, NativeImageBackedTexture
 *   constructors, or InventoryScreen.drawEntity signature).
 */

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;

    // library
    private final List<File> localSkins = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    // preview state
    private float rotYaw = 0f;
    private float rotPitch = 0f;
    private float zoom = 40f; // size param for drawEntity
    private boolean rotating = false;
    private double lastMouseX, lastMouseY;

    // registered texture (so we can free/replace)
    private Identifier customTexId;
    private NativeImageBackedTexture customTexture;

    // file storage
    private final File skinDir;

    public SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
        this.skinDir = new File("config/javaskinchanger/skins");
        if (!skinDir.exists()) skinDir.mkdirs();
    }

    @Override
    protected void init() {
        loadLocalSkins();

        // Upload button
        this.addDrawableChild(new ButtonWidget(10, 10, 140, 20, Text.of("Upload Skin"), btn -> {
            // open JFileChooser on a background thread (UI thread shouldn't block)
            new Thread(() -> {
                File picked = openFileDialog();
                if (picked != null) {
                    // copy into our config folder
                    File dest = new File(skinDir, picked.getName());
                    try (InputStream in = new FileInputStream(picked); OutputStream out = new FileOutputStream(dest)) {
                        byte[] buf = new byte[4096];
                        int r;
                        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                        loadLocalSkins();
                        // apply immediately
                        applySkinFile(dest, "localskin");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }));

        // Fetch Mojang skin button
        this.addDrawableChild(new ButtonWidget(160, 10, 180, 20, Text.of("Fetch Mojang Skin"), btn -> {
            // run fetch off-thread
            new Thread(() -> {
                if (client.player == null) return;
                try {
                    String uuid = client.player.getGameProfile().getId().toString().replace("-", "");
                    String url = net.rabimi.util.MojangFetcher.fetchSkinUrl(uuid);
                    if (url != null) {
                        BufferedImage img = ImageIO.read(new java.net.URL(url));
                        File tmp = new File(skinDir, "mojang_skin_" + client.player.getGameProfile().getName() + ".png");
                        ImageIO.write(img, "png", tmp);
                        // apply
                        applySkinFile(tmp, "mojangskin");
                        loadLocalSkins();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }));

        // quick note: inform user where files go
        // (can't draw a dialog here — rely on chat/log)
        System.out.println("[JavaSkinChanger] Skins folder: " + skinDir.getAbsolutePath());
    }

    private void loadLocalSkins() {
        localSkins.clear();
        if (!skinDir.exists()) skinDir.mkdirs();
        File[] files = skinDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) return;
        for (File f : files) localSkins.add(f);
    }

    // JFileChooser fallback. On Android environments this may not work; use PrismLauncher file access instead.
    private File openFileDialog() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Minecraft skin PNG");
            int res = chooser.showOpenDialog(null);
            if (res == JFileChooser.APPROVE_OPTION) return chooser.getSelectedFile();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    // convert BufferedImage -> NativeImage and register a texture id
    private Identifier registerTextureFromBuffered(BufferedImage img, String idName) throws IOException {
        // ensure 64x64
        BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        resized.getGraphics().drawImage(img, 0, 0, 64, 64, null);

        // create NativeImage
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", os);
        InputStream in = new ByteArrayInputStream(os.toByteArray());
        NativeImage nativeImage = NativeImage.read(in);

        // free previous
        if (customTexture != null && customTexId != null) {
            try { client.getTextureManager().releaseTexture(customTexId); } catch (Throwable ignored) {}
            customTexture.close();
        }

        // create NativeImageBackedTexture — constructor depends on mappings; use Supplier-based constructor
        customTexture = new NativeImageBackedTexture(() -> "javaskinchanger:" + idName, nativeImage);
        customTexId = new Identifier("javaskinchanger", idName);

        client.getTextureManager().registerTexture(customTexId, customTexture);
        return customTexId;
    }

    private void applySkinFile(File file, String idName) {
        try {
            BufferedImage img = ImageIO.read(file);
            Identifier id = registerTextureFromBuffered(img, idName);
            // Apply to player via mixin or available setter. We attempt to call common method via reflection for compatibility.
            client.execute(() -> {
                try {
                    ClientPlayerEntity p = client.player;
                    if (p == null) return;
                    // Many mappings provide setSkinTexture(Identifier). If absent, a mixin that reads our customTexId is needed.
                    try {
                        p.getClass().getMethod("setSkinTexture", Identifier.class).invoke(p, id);
                    } catch (NoSuchMethodException nsme) {
                        // fallback: hacky way — call getSkinTexture if exists? or rely on mixin to swap textures based on UUID.
                        System.out.println("[JavaSkinChanger] setSkinTexture not present on player class — please enable the provided mixin to inject custom textures.");
                    }
                } catch (Throwable t) { t.printStackTrace(); }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // background
        this.renderBackground(context);

        // draw local skin thumbnails horizontally
        int sx = 10 - scrollOffset;
        int y = 50;
        int thumb = 32;
        int pad = 6;

        for (int i = 0; i < localSkins.size(); i++) {
            File f = localSkins.get(i);
            try {
                BufferedImage img = ImageIO.read(f);
                // register per-index texture name
                Identifier tid = new Identifier("javaskinchanger", "thumb_" + i);
                // create native image and texture; small cost but fine for thumbnails
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(img, "png", os);
                NativeImage nimg = NativeImage.read(new ByteArrayInputStream(os.toByteArray()));
                NativeImageBackedTexture ttex = new NativeImageBackedTexture(() -> "javaskinchanger:thumb_" + i, nimg);
                client.getTextureManager().registerTexture(tid, ttex);

                context.drawTexture(tid, sx + i * (thumb + pad), y, 0, 0, img.getWidth(), img.getHeight(), thumb, thumb);

                if (i == selectedIndex) {
                    context.fill(sx + i * (thumb + pad) - 2, y - 2, sx + i * (thumb + pad) + thumb + 2, y + thumb + 2, 0xFFFFFFFF);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // base UI text
        context.drawText(this.textRenderer, Text.of("Local skins: " + localSkins.size()), 10, 34, 0xFFFFFF);
        context.drawText(this.textRenderer, Text.of("Files: " + skinDir.getAbsolutePath()), 10, height - 12, 0xAAAAAA);

        // 3D preview in center
        int centerX = width / 2;
        int centerY = height / 2 + 20;

        if (client.player != null) {
            try {
                // InventoryScreen.drawEntity is usually present and convenient — signature differs across versions.
                // We'll call the common variant: (DrawContext, int x, int y, int size, float yaw, float pitch, LivingEntity entity)
                InventoryScreen.drawEntity(context, centerX, centerY, (int) zoom, rotYaw, rotPitch, client.player);
            } catch (Throwable t) {
                // If the above doesn't exist in your mapping, please adjust to the renderer.render(...) call used in your mappings.
                t.printStackTrace();
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // mouse events for rotate/scroll/thumbnail click
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX; lastMouseY = mouseY;
        if (button == 0) {
            // check if clicked a thumbnail
            int sx = 10 - scrollOffset;
            int y = 50;
            int thumb = 32;
            int pad = 6;
            for (int i = 0; i < localSkins.size(); i++) {
                int x0 = sx + i * (thumb + pad);
                int x1 = x0 + thumb;
                if (mouseX >= x0 && mouseX <= x1 && mouseY >= y && mouseY <= y + thumb) {
                    selectedIndex = i;
                    applySkinFile(localSkins.get(i), "localskin_" + i);
                    return true;
                }
            }
            // otherwise start rotating
            rotating = true;
        } else if (button == 1) {
            // right-click to start scroll-drag
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        rotating = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rotating) {
            rotYaw += deltaX * 0.7f;
            rotPitch += deltaY * 0.7f;
        } else {
            // drag thumbnails horizontally
            scrollOffset -= deltaX;
            if (scrollOffset < 0) scrollOffset = 0;
        }
        lastMouseX = mouseX; lastMouseY = mouseY;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        zoom += amount * 4f;
        if (zoom < 10f) zoom = 10f;
        if (zoom > 120f) zoom = 120f;
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
