package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkinChangeScreen extends Screen {

    private static final Identifier DEFAULT_SKIN = Identifier.of("textures/entity/player/wide/steve.png");
    private Identifier selectedSkin = null;

    public SkinChangeScreen() {
        super(Text.literal("Skin Changer"));
    }

    @Override
    protected void init() {

        // スキン選択ボタン
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("スキンを選択"), (btn) -> {
                    // TODO: ファイル選択画面を開く処理
                }).dimensions(this.width / 2 - 60, this.height / 2 - 20, 120, 20).build()
        );

        // 適用ボタン
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("適用する"), (btn) -> {
                    applySkin();
                }).dimensions(this.width / 2 - 60, this.height / 2 + 10, 120, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // 3Dモデル表示
        renderPlayerModel(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlayerModel(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        int x = this.width / 2;
        int y = this.height / 2 - 60;

        // ↓1.21.5 での 3D エンティティ描画（LivingEntity 不要）
        context.drawEntity(x, y, 45, 0, 0, player);
    }

    private void applySkin() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Identifier skin = selectedSkin != null ? selectedSkin : DEFAULT_SKIN;

        // TODO: スキン変更処理
        System.out.println("Apply Skin: " + skin);

        client.player.sendMessage(Text.literal("スキンを変更しました！"));
    }
}
