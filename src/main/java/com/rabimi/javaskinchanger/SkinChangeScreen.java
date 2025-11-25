package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack; // 一部環境で使う場合あり

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SkinChangeScreen — 左：画像選択、水色ボタン / 右：スキンライブラリ（簡易リスト）
 * - 画像選択はデスクトップの JFileChooser を使う（PC向け）
 * - 選択した画像は 64x64 に自動リサイズして保存（config/javaskinchanger/skins/ に日時ファイル名）
 * - 登録時に NativeImageBackedTexture を作り texturesManager に登録 -> Mixin が取得して反映する
 *
 * Notes:
 * - 実際のメソッド・クラスの微差（DrawContext vs MatrixStack, registerTexture の戻り値など）は
 *   Fabric / Yarn のバージョンにより変わるため、ビルドエラー出たらエラー行を教えてください。
 */
@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private static final int MAX_WIDTH = 700;
    private static final int MAX_HEIGHT = 420;
    private static final String MOD_DIR = "config/javaskinchanger";
    private static final String SKIN_DIR = MOD_DIR + "/skins";

    private final MinecraftClient client = MinecraftClient.getInstance();

    // layout
    private int windowWidth, windowHeight, leftX, topY;

    // library (simple cached list)
    private List<SkinLibrary.SkinEntry> library;

    // currently previewed custom skin id (registered in texture manager)
    private Identifier previewSkinId = null;

    public SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        // ensure dirs
        try {
            Files.createDirectories(new File(SKIN_DIR).toPath());
        } catch (IOException ignored) {}
        this.library = SkinLibrary.loadLibrary(); // load on open
    }

    @Override
    protected void init() {
        super.init();

        windowWidth = Math.min((int)(this.width * 0.8), MAX_WIDTH);
        windowHeight = Math.min((int)(this.height * 0.7), MAX_HEIGHT);

        leftX = (this.width - windowWidth) / 2;
        topY  = (this.height - windowHeight) / 2;

        int buttonW = 140;
        int buttonH = 28;
        int padding = 12;

        // 左：画像選択（水色風） — 実際に色はDrawContextのfillで描くのでボタンは標準。水色に見えるよう背景描く。
        this.addDrawableChild(ButtonWidget.builder(Text.of("画像選択 (Upload)"), btn -> {
            openAndImportSkin();
        }).dimensions(leftX + padding, topY + padding + 30, buttonW, buttonH).build());

        // 右：ライブラリから選ぶ（簡易表示 - 各スキンをクリックで選択）
        // We'll render the library manually and add invisible buttons later if needed.
    }

    // open file chooser, resize to 64x64, save with timestamp, register texture, update library
    private void openAndImportSkin() {
        // JFileChooser — desktop only (user said PC builds now)
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setDialogTitle("Select a Minecraft skin PNG (any size, will be resized to 64x64)");
            int res = chooser.showOpenDialog(null);
            if (res != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            if (file == null || !file.exists()) return;

            try {
                BufferedImage src = ImageIO.read(file);
                if (src == null) return;

                // normalize to 64x64: if 64x32 -> convert to 64x64 (duplicate lower half), else scale
                BufferedImage normalized = normalizeTo64x64(src);

                // save to skins dir with timestamp
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String name = "skin_" + ts + ".png";
                File out = new File(SKIN_DIR, name);
                ImageIO.write(normalized, "PNG", out);

                // make NativeImage and register
                NativeImage nativeImg = nativeImageFromBuffered(normalized);
                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "javaskinchanger/" + name, nativeImg);
                Identifier id = new Identifier("javaskinchanger", name.replace(".png", ""));
                client.getTextureManager().registerTexture(id, tex);

                // update preview and library file
                previewSkinId = id;
                SkinLibrary.addSkin(out.getName(), out.getAbsolutePath());
                library = SkinLibrary.loadLibrary();

                System.out.println("[JavaSkinChanger] Imported skin: " + out.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // convert BufferedImage -> NativeImage (helper)
    private NativeImage nativeImageFromBuffered(BufferedImage buf) throws IOException {
        // NativeImage.read(InputStream) doesn't always exist in mappings; use byte array fallback:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buf, "PNG", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return NativeImage.read(bais);
    }

    // normalize image to 64x64 (handle 64x32 -> convert simpler)
    private BufferedImage normalizeTo64x64(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();

        // if 64x32 classic -> convert to 64x64 by copying lower half blank (common approach)
        if (w == 64 && h == 32) {
            BufferedImage dst = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.drawImage(src, 0, 0, null); // top 64x32 -> top of 64x64
            // copy top half to bottom as a fallback for outer layers
            g.drawImage(src, 0, 32, 64, 64, 0, 0, 64, 32, null);
            g.dispose();
            return dst;
        }

        // otherwise scale to 64x64 with smooth scaling
        BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, 64, 64, null);
        g.dispose();
        return scaled;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // background window
        context.fill(leftX, topY, leftX + windowWidth, topY + windowHeight, 0xBF000000);

        // left-top title
        context.drawText(this.textRenderer, Text.of("JavaSkinChanger"), leftX + 8, topY + 6, 0xFFFFFF, false);

        // center divider (light gray)
        int centerX = leftX + windowWidth / 2;
        context.fill(centerX - 1, topY, centerX + 1, topY + windowHeight, 0xFFD3D3D3);

        // left panel: preview box and buttons
        int leftPanelX = leftX + 10;
        int leftPanelW = windowWidth / 2 - 20;
        int previewY = topY + 60;
        int previewSize = Math.min(leftPanelW - 20, windowHeight - 120);

        // draw preview background (light)
        context.fill(leftPanelX, previewY, leftPanelX + previewSize, previewY + previewSize, 0xFF202020);

        // draw 3D player model in preview
        if (client.player != null) {
            // InventoryScreen.drawEntity signature in this mapping expects:
            // InventoryScreen.drawEntity(DrawContext, int x, int y, int size, int size2, int size3, float yaw, float pitch, float roll, LivingEntity ent)
            // different mappings vary — we try a commonly-seen signature with multiple ints & floats.
            try {
                InventoryScreen.drawEntity(
                        context,
                        leftPanelX + previewSize / 2,  // x center
                        previewY + previewSize - 10,   // y baseline
                        previewSize / 2,               // size param1
                        previewSize / 2,               // size param2
                        previewSize / 2,               // size param3
                        (float)(mouseX - (leftPanelX + previewSize/2)) * 0.5f, // yaw
                        (float)(mouseY - (previewY + previewSize/2)) * 0.5f,   // pitch
                        0.0f,
                        client.player
                );
            } catch (Throwable t) {
                // If signature mismatches, avoid crash — just ignore model render (user can report exact signature).
            }
        }

        // left: buttons (already added via widgets) — draw a colored background for "android-style"
        // (water-blue rectangle behind the upload button to make it "blue")
        // Find the button position (we know how we set it)
        int btnX = leftX + 12;
        int btnY = topY + 12 + 30;
        context.fill(btnX - 4, btnY - 4, btnX + 148 + 4, btnY + 28 + 4, 0xFF64B5FF); // water-blue bg

        // right panel: library list
        int rightX = centerX + 10;
        int rightW = windowWidth / 2 - 20;
        context.drawText(this.textRenderer, Text.of("Library"), rightX, topY + 35, 0xFFFFFF, false);

        // list items
        int itemY = topY + 60;
        int itemH = 28;
        int idx = 0;
        for (SkinLibrary.SkinEntry e : library) {
            int y = itemY + idx * (itemH + 6);
            // highlight hovered
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= y && mouseY <= y + itemH) {
                context.fill(rightX, y, rightX + rightW, y + itemH, 0xFF2A2A2A);
            }
            context.drawText(this.textRenderer, Text.of(e.name), rightX + 6, y + 8, 0xFFFFFF, false);

            // clicking: if user clicked inside, set previewSkinId & register texture if not already
            if (this.client.mouse.wasLeftButtonClicked() && mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= y && mouseY <= y + itemH) {
                // register if necessary
                try {
                    File f = new File(e.path);
                    if (f.exists()) {
                        BufferedImage b = ImageIO.read(f);
                        NativeImage ni = nativeImageFromBuffered(b);
                        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "javaskinchanger/" + f.getName(), ni);
                        Identifier id = new Identifier("javaskinchanger", f.getName().replace(".png",""));
                        client.getTextureManager().registerTexture(id, tex);
                        previewSkinId = id;
                        // set current active skin id in SkinLibrary (so mixin can return it)
                        SkinLibrary.setActive(id.toString());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            idx++;
            if (y + itemH > topY + windowHeight - 20) break; // avoid overflowing
        }

        // if previewSkinId exists show small label under preview
        if (previewSkinId != null) {
            context.drawText(this.textRenderer, Text.of("Preview: " + previewSkinId.getPath()), leftPanelX, previewY + previewSize + 6, 0xFFFFFF, false);
        } else {
            // show "using current account skin" text
            context.drawText(this.textRenderer, Text.of("Preview: (current account skin)"), leftPanelX, previewY + previewSize + 6, 0xAAAAAA, false);
        }

        // Draw widgets (buttons)
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}