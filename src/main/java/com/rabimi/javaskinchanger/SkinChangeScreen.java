package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SkinChangeScreen extends Screen {

    private TextFieldWidget urlField;
    private final MinecraftClient client = MinecraftClient.getInstance();

    public SkinChangeScreen() {
        super(Text.of("Skin Changer"));
    }

    @Override
    protected void init() {
        // テキストフィールド
        urlField = new TextFieldWidget(this.textRenderer, 10, 10, 200, 20, Text.of("Skin URL"));
        this.addDrawableChild(urlField);

        // Apply Skin ボタン
        this.addDrawableChild(new ButtonWidget(
                10, 40, 100, 20,
                Text.of("Apply Skin"),
                button -> {
                    String url = urlField.getText();
                    if (url != null && !url.isEmpty()) {
                        SkinChanger.applySkin(url);
                    }
                },
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));

        // Cancel ボタン
        this.addDrawableChild(new ButtonWidget(
                120, 40, 100, 20,
                Text.of("Cancel"),
                button -> client.setScreen(null),
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // 背景描画
        this.renderBackground(drawContext);

        // URL フィールド描画
        urlField.render(drawContext, mouseX, mouseY, delta);

        // 親クラス描画
        super.render(drawContext, mouseX, mouseY, delta);
    }
}