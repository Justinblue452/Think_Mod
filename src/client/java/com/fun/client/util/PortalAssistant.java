package com.fun.client.util;

public class PortalAssistant {
    public enum ArrowMode { STRONGHOLD, PORTAL }

    private static double targetX = Double.NaN;
    private static double targetZ = Double.NaN;
    private static String targetDim = "";
    private static ArrowMode arrowMode = ArrowMode.STRONGHOLD;

    public static void setTarget(double x, double z, String dim) {
        targetX = x;
        targetZ = z;
        targetDim = dim;
        arrowMode = ArrowMode.PORTAL;
    }

    public static void clearTarget() {
        targetX = Double.NaN;
        targetZ = Double.NaN;
        targetDim = "";
        arrowMode = ArrowMode.STRONGHOLD;
    }

    public static boolean hasTarget() { return !Double.isNaN(targetX); }
    public static double getTargetX() { return targetX; }
    public static double getTargetZ() { return targetZ; }
    public static String getTargetDim() { return targetDim; }
    public static ArrowMode getArrowMode() { return arrowMode; }

    // 供 ConfigManager 调用
    public static void restore(double x, double z, String dim, ArrowMode mode) {
        targetX = x;
        targetZ = z;
        targetDim = dim;
        arrowMode = mode;
    }
}