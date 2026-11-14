package com.fun.client.render;

import com.fun.client.util.HudCache;
import com.fun.client.util.HudConfig;
import com.fun.client.util.StrongholdLocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class NavigationArrowRenderer {
    private static float smoothAngle = 0f;

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        if (client.player == null || !StrongholdLocator.hasValidTarget()) return;

        float target = HudCache.arrowAngle;
        smoothAngle = Mth.lerp(0.25f, smoothAngle, target);
        if (Math.abs(smoothAngle - target) < 0.5f) smoothAngle = target;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        int cx = screenW / 2 + HudConfig.arrowX;
        int cy = screenH - HudConfig.arrowY;

        float rad = (float) Math.toRadians(smoothAngle - 90);
        float size = 10f;

        float x1 = cx + Mth.cos(rad) * size, y1 = cy + Mth.sin(rad) * size;
        float rl = rad + (float) Math.toRadians(140), rr = rad - (float) Math.toRadians(140);
        float x2 = cx + Mth.cos(rl) * size * 0.6f, y2 = cy + Mth.sin(rl) * size * 0.6f;
        float x3 = cx + Mth.cos(rr) * size * 0.6f, y3 = cy + Mth.sin(rr) * size * 0.6f;

        drawTriangle(g, x1, y1, x2, y2, x3, y3, 0xFF000000, 1.5f);
        drawTriangle(g, x1, y1, x2, y2, x3, y3, 0xFFFFFFFF, 0f);
    }

    private static void drawTriangle(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, float x3, float y3, int color, float expand) {
        int minX = (int) Math.floor(Math.min(x1, Math.min(x2, x3)) - expand);
        int maxX = (int) Math.ceil(Math.max(x1, Math.max(x2, x3)) + expand);
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3)) - expand);
        int maxY = (int) Math.ceil(Math.max(y1, Math.max(y2, y3)) + expand);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInTri(x + 0.5f, y + 0.5f, x1, y1, x2, y2, x3, y3, expand))
                    g.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static boolean pointInTri(float px, float py, float ax, float ay, float bx, float by, float cx, float cy, float e) {
        float d1 = sign(px, py, ax, ay, bx, by), d2 = sign(px, py, bx, by, cx, cy), d3 = sign(px, py, cx, cy, ax, ay);
        boolean neg = d1 < -e || d2 < -e || d3 < -e, pos = d1 > e || d2 > e || d3 > e;
        return !(neg && pos);
    }

    private static float sign(float px, float py, float ax, float ay, float bx, float by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }
}