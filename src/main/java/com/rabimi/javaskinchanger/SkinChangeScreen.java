package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SkinChangeScreen extends Screen {

    private TextFieldWidget urlField;

    protected SkinChangeScreen() {
        super(Text.literal("Skin Changer"));
    }

    @Override
    protected void init() {

        int center = this.width / 2;

        urlField = new TextFieldWidget(textRenderer, center - 100, 70, 200, 20, Text.literal("URL"));
        addSelectableChild(urlField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply Skin"), btn -> {
            String url = urlField.getText();
            SkinChanger.applySkin(url);
        }).dimensions(center - 40, 110, 80, 20).build());
    }
}