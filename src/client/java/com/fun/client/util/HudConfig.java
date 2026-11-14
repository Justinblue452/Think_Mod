package com.fun.client.util;

import java.util.HashSet;
import java.util.Set;

public class HudConfig {
    public static boolean hudVisible = true;
    public static boolean showArrow = true;
    public static boolean showInfo = true;

    public static int arrowX = 0;
    public static int arrowY = 75;
    public static int infoX = 10;
    public static int infoY = 120;

    // 实体警告配置
    public static boolean showEntityWarning = true;
    public static Set<String> warningEntityTypes = new HashSet<>(Set.of("minecraft:creeper"));
    public static double detectRange = 16.0;
    public static double warningRange = 8.0;
    public static double dangerRange = 4.0;

    // 最多同时显示的警告实体数量（防止刷怪塔导致 HUD 爆炸）
    public static int maxWarningEntities = 16;
}