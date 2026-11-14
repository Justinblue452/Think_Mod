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
    // 推荐加上 serializeSpecialFloatingPointValues() 作为双重保险，防止未来有其他 NaN 写入
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

        boolean showEntityWarning = true;
        List<String> warningEntityTypes = new ArrayList<>(List.of("minecraft:creeper"));
        double detectRange = 16.0;
        double warningRange = 8.0;
        double dangerRange = 4.0;
        int maxWarningEntities = 6;

        // 传送门目标持久化
        // 改用 Double 包装类，如果未设置目标，就会序列化为 null（JSON 中直接省略该字段）
        Double portalX = null;
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
                    HudConfig.showEntityWarning = d.showEntityWarning;
                    HudConfig.warningEntityTypes = new HashSet<>(d.warningEntityTypes);
                    HudConfig.detectRange = d.detectRange;
                    HudConfig.warningRange = d.warningRange;
                    HudConfig.dangerRange = d.dangerRange;
                    HudConfig.maxWarningEntities = d.maxWarningEntities;

                    // 恢复传送门目标：判断是否为空
                    if (d.portalX != null && d.portalZ != null) {
                        PortalAssistant.ArrowMode mode = "PORTAL".equals(d.arrowMode)
                                ? PortalAssistant.ArrowMode.PORTAL : PortalAssistant.ArrowMode.STRONGHOLD;
                        PortalAssistant.restore(d.portalX, d.portalZ, d.portalDim, mode);
                    } else {
                        // 如果没有记录，确保助手状态是清空的
                        PortalAssistant.clearTarget();
                    }
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
            d.arrowX = HudConfig.arrowX;
            d.arrowY = HudConfig.arrowY;
            d.showEntityWarning = HudConfig.showEntityWarning;
            d.warningEntityTypes = new ArrayList<>(HudConfig.warningEntityTypes);
            d.detectRange = HudConfig.detectRange;
            d.warningRange = HudConfig.warningRange;
            d.dangerRange = HudConfig.dangerRange;
            d.maxWarningEntities = HudConfig.maxWarningEntities;

            // 转换 NaN 为 null，防止 Gson 崩溃
            double tx = PortalAssistant.getTargetX();
            double tz = PortalAssistant.getTargetZ();
            d.portalX = Double.isNaN(tx) ? null : tx;
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