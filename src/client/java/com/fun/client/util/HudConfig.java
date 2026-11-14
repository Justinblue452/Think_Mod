package com.fun.client.util;

import java.util.HashSet;
import java.util.Set;

public class HudConfig {
    public static boolean hudVisible = true;
    public static boolean showArrow = true;

    public static int arrowX = 0;
    public static int arrowY = 75;

    public static boolean showYAxis = true;
    public static boolean soundEnabled = true;

    public static boolean showEntityWarning = true;
    public static Set<String> warningEntityTypes = new HashSet<>(Set.of("minecraft:creeper"));
    public static double detectRange = 16.0;
    public static double warningRange = 8.0;
    public static double dangerRange = 4.0;
    public static int maxWarningEntities = 6;

    // 音效类型: "pling"（音符盒）、"orb"（经验球）、"bit"（8-bit 电子音）、"bell"（铃铛）
    public static String warningSound = "pling";
}