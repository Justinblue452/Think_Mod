package com.fun.client.util;

public class PortalAssistant {
    public enum ArrowMode { STRONGHOLD, PORTAL }

    private static double targetX = Double.NaN;
    private static double targetY = Double.NaN;
    private static double targetZ = Double.NaN;
    private static String targetDim = "";
    private static ArrowMode arrowMode = ArrowMode.STRONGHOLD;

    /** 完整版本：同时记录 X/Y/Z */
    public static void setTarget(double x, double y, double z, String dim) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetDim = dim;
        arrowMode = ArrowMode.PORTAL;
    }

    /** 兼容旧调用：默认 Y = 64 */
    public static void setTarget(double x, double z, String dim) {
        setTarget(x, 64, z, dim);
    }

    public static void clearTarget() {
        targetX = Double.NaN;
        targetY = Double.NaN;
        targetZ = Double.NaN;
        targetDim = "";
        arrowMode = ArrowMode.STRONGHOLD;
    }

    public static boolean hasTarget() { return !Double.isNaN(targetX); }
    public static double getTargetX() { return targetX; }
    public static double getTargetY() { return targetY; }
    public static double getTargetZ() { return targetZ; }
    public static String getTargetDim() { return targetDim; }
    public static ArrowMode getArrowMode() { return arrowMode; }

    public static void restore(double x, double y, double z, String dim, ArrowMode mode) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetDim = dim;
        arrowMode = mode;
    }

    /** 兼容旧 4 参数 restore */
    public static void restore(double x, double z, String dim, ArrowMode mode) {
        restore(x, 64, z, dim, mode);
    }
}