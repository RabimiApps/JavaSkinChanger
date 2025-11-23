package com.rabimi.javaskinchanger;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class SkinChangeScreen extends Screen {

    private TextFieldWidget urlField;

    protected SkinChangeScreen() {
        super(Text.of("Skin Changer"));
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();

        // URL入力欄を作成
        urlField = new TextFieldWidget(this.textRenderer, 10, 10, 200, 20, Text.of("Skin URL"));
        this.addDrawableChild(urlField);

        // ボタンを作成
        this.addDrawableChild(new ButtonWidget(10, 40, 100, 20, Text.of("Apply Skin"), button -> {
            String url = urlField.getText();
            if (url != null && !url.isEmpty()) {
                SkinChanger.applySkin(url);
            }
        }));

        // キャンセルボタン
        this.addDrawableChild(new ButtonWidget(120, 40, 100, 20, Text.of("Cancel"), button -> {
            client.setScreen(null); // 前の画面に戻る
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        urlField.render(mouseX, mouseY, delta);
        super.render(mouseX, mouseY, delta);
    }
}