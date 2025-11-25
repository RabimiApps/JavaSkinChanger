package com.rabimi.javaskinchanger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Very small JSON-backed skin library.
 * Stored at config/javaskinchanger/skins.json
 */
public class SkinLibrary {

    public static class SkinEntry {
        public String name;
        public String path;
        public SkinEntry() {}
        public SkinEntry(String name, String path){ this.name = name; this.path = path; }
    }

    private static final File LIB_FILE = new File("config/javaskinchanger/skins.json");
    private static final Gson GSON = new Gson();

    public static List<SkinEntry> loadLibrary(){
        try {
            if (!LIB_FILE.exists()) return new ArrayList<>();
            Reader r = new FileReader(LIB_FILE);
            Type listType = new TypeToken<List<SkinEntry>>(){}.getType();
            List<SkinEntry> list = GSON.fromJson(r, listType);
            r.close();
            if (list == null) return new ArrayList<>();
            return list;
        } catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void addSkin(String name, String path){
        try {
            List<SkinEntry> list = loadLibrary();
            list.add(new SkinEntry(name, path));
            try (Writer w = new FileWriter(LIB_FILE)) {
                GSON.toJson(list, w);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setActive(String identifierString){
        // simple placeholder — for real usage you may write active info to a separate file
        // e.g. config/javaskinchanger/active.txt
        try {
            File f = new File("config/javaskinchanger/active.txt");
            f.getParentFile().mkdirs();
            try (Writer w = new FileWriter(f)) {
                w.write(identifierString);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static String getActive(){
        try {
            File f = new File("config/javaskinchanger/active.txt");
            if (!f.exists()) return null;
            byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
            return new String(b);
        } catch (Exception e){
            return null;
        }
    }
}