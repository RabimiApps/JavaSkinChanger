package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.function.Consumer;

public class FileDialogHelper {

    /**
     * TinyFileDialogs によるクロスプラットフォームファイル選択
     */
    public static void open(String title, Consumer<String> callback) {

        MinecraftClient.getInstance().executeAsync(() -> {

            String path = TinyFileDialogs.tinyfd_openFileDialog(
                    title,
                    "",
                    new String[]{"*.png"},
                    "PNG Image",
                    false
            );

            callback.accept(path);
        });
    }
}