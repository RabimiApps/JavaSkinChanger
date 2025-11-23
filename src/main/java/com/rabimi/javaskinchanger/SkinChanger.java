package com.rabimi.javaskinchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.net.HttpURLConnection;
import java.net.URL;

public class SkinChanger {

    public static void applySkin(String skinUrl) {
        new Thread(() -> {
            try {
                String token = MinecraftClient.getInstance().getSession().getAccessToken();

                String body = "{ \"variant\": \"classic\", \"url\": \"" + skinUrl + "\" }";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://api.minecraftservices.com/minecraft/profile/skins").openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                conn.getOutputStream().write(body.getBytes());

                System.out.println("Skin API Response: " + conn.getResponseCode());

                MinecraftClient.getInstance().execute(() -> {
                    ClientPlayerEntity p = MinecraftClient.getInstance().player;
                    if (p != null) p.refreshSkin();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}