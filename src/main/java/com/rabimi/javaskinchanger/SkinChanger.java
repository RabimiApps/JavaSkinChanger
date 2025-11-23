package com.rabimi.javaskinchanger;

import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.client.MinecraftClient;

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

                int responseCode = conn.getResponseCode();
                System.out.println("Skin API Response: " + responseCode);

                if (responseCode == 200 || responseCode == 204) {
                    mc.execute(() -> {
                        mc.player.sendMessage(
                            Text.of("スキンをアップロードしました！ワールドまたはサーバーを再参加してください。"), false
                        );
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}