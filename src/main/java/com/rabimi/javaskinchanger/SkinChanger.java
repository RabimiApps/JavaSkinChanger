package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;

import java.net.HttpURLConnection;
import java.net.URL;

public class SkinChanger {

    public static void applySkin(String skinUrl) {
        new Thread(() -> {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                String token = mc.getSession().getAccessToken();

                String body = "{ \"variant\": \"classic\", \"url\": \"" + skinUrl + "\" }";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://api.minecraftservices.com/minecraft/profile/skins").openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                conn.getOutputStream().write(body.getBytes());

                System.out.println("Skin API Response: " + conn.getResponseCode());

                // Minecraft 1.21.5では refreshSkin がないので SkinProvider.reload() を使用
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.getSkinProvider().reload();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}