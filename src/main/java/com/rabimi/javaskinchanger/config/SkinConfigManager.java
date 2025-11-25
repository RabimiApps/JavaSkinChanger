package com.rabimi.javaskinchanger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabimi.javaskinchanger.SkinCache;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SkinConfigManager {

    private static final File CONFIG_DIR = new File("config/jsc");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "skins.json");

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static SkinData data = new SkinData();

    public static class SkinData {
        public List<String> skins = new ArrayList<>();
        public int selected = 0;
    }

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                save();
                return;
            }
            data = gson.fromJson(new FileReader(CONFIG_FILE), SkinData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            CONFIG_DIR.mkdirs();
            FileWriter writer = new FileWriter(CONFIG_FILE);
            gson.toJson(data, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addSkin(String texturePath) {
        data.skins.add(texturePath);
        save();
    }

    public static void select(int index) {
        data.selected = index;
        save();
    }

    public static Identifier getSelectedSkinId() {
        if (data.skins.isEmpty()) return null;
        return new Identifier("javaskinchanger", "slot_" + data.selected);
    }
}