package com.fun.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("think.json");

    private static class Data {
        boolean hudVisible = true;
        boolean showArrow = true;
        boolean showInfo = true;
        int arrowX = 0;
        int arrowY = 75;
        int infoX = 10;
        int infoY = 120;

        boolean showEntityWarning = true;
        List<String> warningEntityTypes = new ArrayList<>(List.of("minecraft:creeper"));
        double detectRange = 16.0;
        double warningRange = 8.0;
        double dangerRange = 4.0;
        int maxWarningEntities = 16;
    }

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) { save(); return; }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Data d = GSON.fromJson(reader, Data.class);
                if (d != null) {
                    HudConfig.hudVisible = d.hudVisible;
                    HudConfig.showArrow = d.showArrow;
                    HudConfig.showInfo = d.showInfo;
                    HudConfig.arrowX = d.arrowX;
                    HudConfig.arrowY = d.arrowY;
                    HudConfig.infoX = d.infoX;
                    HudConfig.infoY = d.infoY;

                    HudConfig.showEntityWarning = d.showEntityWarning;
                    HudConfig.warningEntityTypes = new HashSet<>(d.warningEntityTypes);
                    HudConfig.detectRange = d.detectRange;
                    HudConfig.warningRange = d.warningRange;
                    HudConfig.dangerRange = d.dangerRange;
                    HudConfig.maxWarningEntities = d.maxWarningEntities;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Data d = new Data();
            d.hudVisible = HudConfig.hudVisible;
            d.showArrow = HudConfig.showArrow;
            d.showInfo = HudConfig.showInfo;
            d.arrowX = HudConfig.arrowX;
            d.arrowY = HudConfig.arrowY;
            d.infoX = HudConfig.infoX;
            d.infoY = HudConfig.infoY;

            d.showEntityWarning = HudConfig.showEntityWarning;
            d.warningEntityTypes = new ArrayList<>(HudConfig.warningEntityTypes);
            d.detectRange = HudConfig.detectRange;
            d.warningRange = HudConfig.warningRange;
            d.dangerRange = HudConfig.dangerRange;
            d.maxWarningEntities = HudConfig.maxWarningEntities;

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(d, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}