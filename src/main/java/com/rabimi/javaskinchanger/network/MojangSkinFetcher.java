package com.rabimi.javaskinchanger.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabimi.javaskinchanger.SkinCache;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class MojangSkinFetcher {

    public static void fetch() {
        try {
            String token = MinecraftClient.getInstance().getSession().getAccessToken();

            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);

            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonObject();

            String texBase64 = json.get("skins")
                    .getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("texture_data").getAsString();

            byte[] decoded = Base64.getDecoder().decode(texBase64);

            Identifier id = SkinCache.registerByteImage("official", decoded);
            SkinCache.customSkin = id;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}