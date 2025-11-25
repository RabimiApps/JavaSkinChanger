package com.rabimi.javaskinchanger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class SkinChangeScreen extends Screen {

    private final MinecraftClient client;

    // モデル回転用
    private float modelYaw = 0;
    private float prevMouseX;
    private boolean dragging = false;

    // 新しいスキンID
    private Identifier previewSkinId = null;

    protected SkinChangeScreen() {
        super(Text.of("JavaSkinChanger"));
        this.client = MinecraftClient.getInstance();
    }

    @Override
    protected void init() {

        // ファイル選択ボタン
        addDrawableChild(ButtonWidget.builder(Text.of("Select Skin PNG"), btn -> selectSkin())
                .dimensions(10, 10, 150, 20).build());

        // 適用ボタン
        addDrawableChild(ButtonWidget.builder(Text.of("Apply"), btn -> applySkin())
                .dimensions(10, 40, 150, 20).build());
    }

    /**
     * Cross-platform ファイル選択（Linux/Android/Switchrootでも動く）
     */
    private void selectSkin() {
        FileDialogHelper.open("Select PNG file", path -> {
            if (path == null) return;

            File f = new File(path);
            loadPreviewSkin(f);

            // config/jsc/skin.png に保存
            File target = new File("config/jsc/skin.png");
            target.getParentFile().mkdirs();
            try {
                java.nio.file.Files.copy(f.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * PNG → 64x64 → Texture登録
     */
    private void loadPreviewSkin(File file) {
        try {
            NativeImage img = NativeImage.read(file);
            NativeImage resized = new NativeImage(64, 64, true);
            img.resizeTo(resized);
            img.close();
            img = resized;

            previewSkinId = new Identifier("javaskinchanger", "preview");

            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            client.getTextureManager().registerTexture(previewSkinId, tex);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mixinへ適用
     */
    private void applySkin() {
        SkinCache.customSkin = previewSkinId;
    }

    @Override
    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        renderBackground(dc);

        super.render(dc, mouseX, mouseY, delta);

        // --- 3Dモデル ---
        drawPlayer(dc);

        // --- 現在使用中のスキン or プレビュー ---
        Identifier skin = (previewSkinId != null)
                ? previewSkinId
                : client.player.getSkinTexture();

        dc.drawTexture(skin, width - 100, 20, 0, 0, 64, 64, 64, 64);
    }

    private void drawPlayer(DrawContext dc) {
        if (client.player == null) return;

        int x = width / 2 - 50;
        int y = height - 20;

        // InventoryScreen の公式メソッド（最新版）
        dc.drawEntity(x, y, 60, modelYaw, -modelYaw, client.player);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        modelYaw += dx * 0.5f;
        return true;
    }
}