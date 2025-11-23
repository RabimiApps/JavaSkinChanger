package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.net.HttpURLConnection;
import java.net.URL;

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

                // ===== ここが重要：refreshSkin が 1.21.5 には無いので SkinProvider.reload() =====
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.getSkinProvider().reload();  // ← これが正しい更新処理
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }