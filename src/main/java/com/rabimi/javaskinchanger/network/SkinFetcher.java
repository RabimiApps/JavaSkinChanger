package com.rabimi.javaskinchanger.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URL;
import java.util.Base64;
import java.util.Scanner;

public class SkinFetcher {

    public static String fetchSkinUrl(String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            Scanner scanner = new Scanner(url.openStream());
            String json = scanner.useDelimiter("\\A").next();
            scanner.close();

            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonObject properties = obj.getAsJsonArray("properties").get(0).getAsJsonObject();
            String value = properties.get("value").getAsString();

            String decoded = new String(Base64.getDecoder().decode(value));
            JsonObject tex = JsonParser.parseString(decoded).getAsJsonObject();

            return tex.get("textures").getAsJsonObject()
                    .get("SKIN").getAsJsonObject()
                    .get("url").getAsString();

        } catch (Exception ignored) {}
        return null;
    }
}