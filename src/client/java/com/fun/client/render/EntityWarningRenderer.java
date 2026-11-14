package com.fun.client.render;

import com.fun.client.util.EntityWarningDetector;
import com.fun.client.util.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EntityWarningRenderer {

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!HudConfig.hudVisible || !HudConfig.showEntityWarning) return;
        if (!EntityWarningDetector.hasThreat()) return;

        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null) return;

        int W = client.getWindow().getGuiScaledWidth();
        int H = client.getWindow().getGuiScaledHeight();
        int cx = W / 2;
        int cy = H / 2;

        double fov = client.options.fov().get();
        double aspectRatio = (double) W / H;
        double tanHalfFovY = Math.tan(Math.toRadians(fov / 2.0));
        double tanHalfFovX = tanHalfFovY * aspectRatio;

        // 统一的呼吸脉冲
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.4 + 0.6);

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 forward = player.getViewVector(1.0f);
        Vec3 up = player.getUpVector(1.0f);
        Vec3 right = forward.cross(up).normalize();

        List<EdgeIndicator> edgeIndicators = new ArrayList<>();

        for (var target : EntityWarningDetector.getTargets()) {
            Entity entity = target.entity();
            float intensity = target.intensity();

            Vec3 targetCenter = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            Vec3 dir = targetCenter.subtract(eyePos).normalize();

            double x = dir.dot(right);
            double y = dir.dot(up);
            double z = dir.dot(forward);

            boolean inView = z > 0.1 && (Math.abs(x) < z * tanHalfFovX) && (Math.abs(y) < z * tanHalfFovY);

            // 每个目标独立计算亮度（越近越亮）
            float brightness = intensity >= 0.95f ? 1.0f : pulse * intensity;
            brightness = Math.max(0.2f, brightness);
            int alpha = (int) (255 * brightness);
            int mainRed = (alpha << 24) | 0xFF0000;
            int innerRed = (alpha << 24) | 0xAA0000;
            int white = (alpha << 24) | 0xFFFFFF;
            int bgRed = ((int) (alpha * 0.4f) << 24) | 0xFF0000;

            if (inView) {
                double screenX = cx + (x / z) / tanHalfFovX * cx;
                double screenY = cy - (y / z) / tanHalfFovY * cy;

                int boxW = 20;
                int boxH = 30;
                int left = (int) (screenX - boxW / 2.0);
                int top = (int) (screenY - boxH / 2.0);

                g.fill(left, top, left + boxW, top + boxH, bgRed);
                g.fill(left, top, left + boxW, top + 2, mainRed);
                g.fill(left, top + boxH - 2, left + boxW, top + boxH, mainRed);
                g.fill(left, top, left + 2, top + boxH, mainRed);
                g.fill(left + boxW - 2, top, left + boxW, top + boxH, mainRed);
            } else {
                double angle;
                if (z > 0) {
                    angle = Math.atan2(y, x);
                } else {
                    if (Math.abs(x) > Math.abs(y)) {
                        angle = x > 0 ? 0 : Math.PI;
                    } else {
                        angle = y > 0 ? Math.PI / 2 : -Math.PI / 2;
                    }
                    angle += Math.atan2(y, Math.abs(x)) * 0.2;
                }
                edgeIndicators.add(new EdgeIndicator(angle, mainRed, innerRed, white));
            }
        }

        // === 边缘指示器防重叠处理 ===
        // 按角度排序，如果相邻两个过于接近，则强制拉开 15 度
        edgeIndicators.sort(Comparator.comparingDouble(i -> i.angle));
        double lastAngle = -Double.MAX_VALUE;
        double minGap = Math.toRadians(15);
        for (var ind : edgeIndicators) {
            double finalAngle = ind.angle;
            if (lastAngle != -Double.MAX_VALUE && finalAngle - lastAngle < minGap) {
                finalAngle = lastAngle + minGap;
            }
            lastAngle = finalAngle;

            double dx = Math.cos(finalAngle);
            double dy = Math.sin(finalAngle);
            double maxX = W / 2.0 - 30;
            double maxY = H / 2.0 - 30;
            double scale = Math.min(Math.abs(maxX / dx), Math.abs(maxY / dy));

            int edgeX = (int) (cx + dx * scale);
            int edgeY = (int) (cy - dy * scale);

            drawEdgeIndicator(g, edgeX, edgeY, finalAngle, ind.mainColor, ind.innerColor, ind.textColor);
        }
    }

    private record EdgeIndicator(double angle, int mainColor, int innerColor, int textColor) {}

    private static void drawEdgeIndicator(GuiGraphicsExtractor g, int x, int y, double angle,
                                          int mainColor, int innerColor, int textColor) {
        int size = 12;
        drawCircle(g, x, y, size, mainColor);
        drawCircle(g, x, y, size - 2, innerColor);

        g.fill(x - 1, y - 5, x + 1, y + 2, textColor);
        g.fill(x - 1, y + 4, x + 1, y + 6, textColor);

        int arrowLength = 8;
        int ax = x + (int) (Math.cos(angle) * (size + arrowLength));
        int ay = y - (int) (Math.sin(angle) * (size + arrowLength));
        int ax1 = x + (int) (Math.cos(angle) * size);
        int ay1 = y - (int) (Math.sin(angle) * size);

        drawLine(g, ax1, ay1, ax, ay, mainColor, 2);
    }

    private static void drawCircle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                if (x * x + y * y <= r * r) {
                    g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
    }

    private static void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy, e2;
        while (true) {
            g.fill(x0 - thickness / 2, y0 - thickness / 2, x0 + thickness / 2 + 1, y0 + thickness / 2 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }
}