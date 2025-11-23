package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class SkinChangeScreen extends Screen {

    private TextFieldWidget urlField;

    public SkinChangeScreen() {
        super(Text.of("Change Skin"));
    }

    @Override
    protected void init() {
        // テキストフィールドの位置とサイズを設定
        this.urlField = new TextFieldWidget(this.textRenderer, 10, 10, 200, 20, Text.of("Skin URL"));
        this.addDrawableChild(this.urlField);

        // Apply Skin ボタン
        this.addDrawableChild(new ButtonWidget(
                10, 40, 100, 20,
                Text.of("Apply Skin"),
                button -> SkinChanger.applySkin(urlField.getText()),
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER // ここは protected なので同パッケージか別実装に変更する必要あり
        ));

        // Cancel ボタン
        this.addDrawableChild(new ButtonWidget(
                120, 40, 100, 20,
                Text.of("Cancel"),
                button -> this.client.setScreen(null),
                ButtonWidget.DEFAULT_NARRATION_SUPPLIER
        ));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // 背景を描画
        this.renderBackground(drawContext, mouseX, mouseY, delta);

        // テキストフィールドを描画
        this.urlField.render(drawContext, mouseX, mouseY, delta);

        super.render(drawContext, mouseX, mouseY, delta);
    }
}