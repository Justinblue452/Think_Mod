package com.fun.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("think.json");

    private static class Data {
        boolean hudVisible = true;
        boolean showArrow = true;
        int arrowX = 0;
        int arrowY = 75;
        boolean showYAxis = true;
        boolean soundEnabled = true;

        boolean showEntityWarning = true;
        List<String> warningEntityTypes = new ArrayList<>(List.of("minecraft:creeper"));
        double detectRange = 16.0;
        double warningRange = 8.0;
        double dangerRange = 4.0;
        int maxWarningEntities = 6;
        String warningSound = "pling";

        Double portalX = null;
        Double portalY = null;
        Double portalZ = null;
        String portalDim = "";
        String arrowMode = "STRONGHOLD";
    }

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) { save(); return; }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Data d = GSON.fromJson(reader, Data.class);
                if (d != null) {
                    HudConfig.hudVisible = d.hudVisible;
                    HudConfig.showArrow = d.showArrow;
                    HudConfig.arrowX = d.arrowX;
                    HudConfig.arrowY = d.arrowY;
                    HudConfig.showYAxis = d.showYAxis;
                    HudConfig.soundEnabled = d.soundEnabled;
                    HudConfig.showEntityWarning = d.showEntityWarning;
                    HudConfig.warningEntityTypes = d.warningEntityTypes != null
                            ? new HashSet<>(d.warningEntityTypes)
                            : new HashSet<>(Set.of("minecraft:creeper"));
                    HudConfig.detectRange = d.detectRange;
                    HudConfig.warningRange = d.warningRange;
                    HudConfig.dangerRange = d.dangerRange;
                    HudConfig.maxWarningEntities = d.maxWarningEntities;
                    if (d.warningSound != null) HudConfig.warningSound = d.warningSound;

                    if (d.portalX != null && d.portalZ != null) {
                        PortalAssistant.ArrowMode mode = "PORTAL".equals(d.arrowMode)
                                ? PortalAssistant.ArrowMode.PORTAL : PortalAssistant.ArrowMode.STRONGHOLD;
                        double y = d.portalY != null ? d.portalY : 64.0;
                        PortalAssistant.restore(d.portalX, y, d.portalZ, d.portalDim, mode);
                    } else {
                        PortalAssistant.clearTarget();
                    }
                }
            }
        } catch (IOException | JsonParseException e) {
            System.err.println("[Think] 配置文件损坏，重置为默认配置...");
            e.printStackTrace();
            save();
        } catch (Exception e) {
            System.err.println("[Think] 加载配置失败，重置为默认配置...");
            e.printStackTrace();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Data d = new Data();
            d.hudVisible = HudConfig.hudVisible;
            d.showArrow = HudConfig.showArrow;
            d.arrowX = HudConfig.arrowX;
            d.arrowY = HudConfig.arrowY;
            d.showYAxis = HudConfig.showYAxis;
            d.soundEnabled = HudConfig.soundEnabled;
            d.showEntityWarning = HudConfig.showEntityWarning;
            d.warningEntityTypes = HudConfig.warningEntityTypes != null
                    ? new ArrayList<>(HudConfig.warningEntityTypes) : new ArrayList<>();
            d.detectRange = HudConfig.detectRange;
            d.warningRange = HudConfig.warningRange;
            d.dangerRange = HudConfig.dangerRange;
            d.maxWarningEntities = HudConfig.maxWarningEntities;
            d.warningSound = HudConfig.warningSound;

            double tx = PortalAssistant.getTargetX();
            double ty = PortalAssistant.getTargetY();
            double tz = PortalAssistant.getTargetZ();
            d.portalX = Double.isNaN(tx) ? null : tx;
            d.portalY = Double.isNaN(ty) ? null : ty;
            d.portalZ = Double.isNaN(tz) ? null : tz;
            d.portalDim = PortalAssistant.getTargetDim();
            d.arrowMode = PortalAssistant.getArrowMode().name();

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(d, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}