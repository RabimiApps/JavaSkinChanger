package com.rabimi.javaskinchanger.util;

import net.minecraft.client.texture.NativeImage;

public class SkinUtil {

    public static NativeImage resizeTo64x64(NativeImage src) {

        NativeImage resized = new NativeImage(64, 64, true);

        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int rgb = src.getColor(x * src.getWidth() / 64, y * src.getHeight() / 64);
                resized.setColor(x, y, rgb);
            }
        }
        return resized;
    }
}