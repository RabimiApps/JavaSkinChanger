package com.rabimi.javaskinchanger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class SkinConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static List<String> skinPaths = new ArrayList<>();

    public static void init(File configDir) {
        configFile = new File(configDir, "skins.json");
        load();
    }

    public static void load() {
        try {
            if (!configFile.exists()) {
                save();
                return;
            }
            SkinConfigManager data = GSON.fromJson(new FileReader(configFile), SkinConfig.class);
            if (data != null) skinPaths = data.skinPaths;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(new SkinConfigManager(), writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}