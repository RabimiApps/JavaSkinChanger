package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.narration.NarrationSupplier;

public class SkinChangeScreen extends Screen {

    private TextFieldWidget urlField;

    public SkinChangeScreen() {
        super(Text.of("Change Skin"));
    }

    @Override
    protected void init() {
        this.urlField = new TextFieldWidget(this.textRenderer, 10, 10, 200, 20, Text.of("Skin URL"));
        this.addDrawableChild(this.urlField);

        // ButtonWidget用の自作 NarrationSupplier
        NarrationSupplier narration = () -> {
            NarrationMessageBuilder builder = new NarrationMessageBuilder();
            builder.append(Text.of("Button"));
            return builder;
        };

        // Apply Skin ボタン
        this.addDrawableChild(new ButtonWidget(
                10, 40, 100, 20,
                Text.of("Apply Skin"),
                button -> SkinChanger.applySkin(urlField.getText()),
                narration
        ));

        // Cancel ボタン
        this.addDrawableChild(new ButtonWidget(
                120, 40, 100, 20,
                Text.of("Cancel"),
                button -> this.client.setScreen(null),
                narration
        ));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext, mouseX, mouseY, delta);
        this.urlField.render(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}