package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkinChangeScreen extends Screen {

    private static final int WINDOW_WIDTH = 450;
    private static final int WINDOW_HEIGHT = 275;

    private int leftX;
    private int topY;

    private final MinecraftClient client = MinecraftClient.getInstance();

    // ダミーのスキン
    private final Identifier DEFAULT_SKIN = Identifier.tryParse("minecraft:textures/entity/steve.png");
    private final Identifier ALEX_SKIN = Identifier.tryParse("minecraft:textures/entity/alex.png");

    public SkinChangeScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        leftX = (this.width - WINDOW_WIDTH) / 2; 
        topY  = (this.height - WINDOW_HEIGHT)/ 2;

        // Change Skin ボタン（ローカル表示のみ）
        this.addDrawableChild(ButtonWidget.builder(Text.of("Change Skin"), button -> {
            client.player.sendMessage(Text.of("Change Skin押された！"), false);
        }).dimensions(leftX + 10, topY + 40, 80, 20).build());

        // Reset Skin ボタン（ローカル表示のみ）
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Skin"), button -> {
            client.player.sendMessage(Text.of("Reset Skin押された！"), false);
        }).dimensions(leftX + 110, topY + 40, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(leftX, topY, leftX + WINDOW_WIDTH, topY + WINDOW_HEIGHT, 0xBF000000);
        context.drawText(this.textRenderer, this.title, leftX + 5, topY + 5, 0xFFFFFF, false);
        super.render(context, mouseX, mouseY, delta);
    }
}
