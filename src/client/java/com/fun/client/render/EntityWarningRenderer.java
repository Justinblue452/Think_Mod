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

        float pulse = (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.4 + 0.6);

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 forward = player.getViewVector(1.0f);
        Vec3 up = player.getUpVector(1.0f);
        Vec3 right = forward.cross(up).normalize();

        // 视野内的红框直接画（这些数量少，不做合并）
        List<EdgeCandidate> edgeCandidates = new ArrayList<>();

        for (var target : EntityWarningDetector.getTargets()) {
            Entity entity = target.entity();
            float intensity = target.intensity();

            Vec3 center = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
            Vec3 dir = center.subtract(eyePos).normalize();

            double x = dir.dot(right);
            double y = dir.dot(up);
            double z = dir.dot(forward);

            boolean inView = z > 0.1 && (Math.abs(x) < z * tanHalfFovX) && (Math.abs(y) < z * tanHalfFovY);

            float brightness = intensity >= 0.95f ? 1.0f : pulse * intensity;
            brightness = Math.max(0.2f, brightness);
            int alpha = (int) (255 * brightness);
            int mainRed = (alpha << 24) | 0xFF0000;
            int bgRed = ((int) (alpha * 0.4f) << 24) | 0xFF0000;

            if (inView) {
                double screenX = cx + (x / z) / tanHalfFovX * cx;
                double screenY = cy - (y / z) / tanHalfFovY * cy;
                int boxW = 20, boxH = 30;
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
                    if (Math.abs(x) > Math.abs(y)) angle = x > 0 ? 0 : Math.PI;
                    else angle = y > 0 ? Math.PI / 2 : -Math.PI / 2;
                    angle += Math.atan2(y, Math.abs(x)) * 0.2;
                }
                edgeCandidates.add(new EdgeCandidate(angle, intensity, mainRed));
            }
        }

        if (edgeCandidates.isEmpty()) return;

        // === 角度分组合并 ===
        edgeCandidates.sort(Comparator.comparingDouble(EdgeCandidate::angle));
        List<MergedIndicator> merged = new ArrayList<>();
        double mergeThreshold = Math.toRadians(12);

        for (var c : edgeCandidates) {
            boolean consumed = false;
            for (var m : merged) {
                if (Math.abs(angleDiff(m.angle, c.angle)) < mergeThreshold) {
                    m.count++;
                    m.intensity = Math.max(m.intensity, c.intensity);
                    consumed = true;
                    break;
                }
            }
            if (!consumed) {
                merged.add(new MergedIndicator(c.angle, 1, c.intensity, c.mainColor));
            }
        }

        // === 防止合并后相邻指示器仍然重叠：强制拉开最小角度 ===
        double minGap = Math.toRadians(15);
        double lastAngle = Double.NEGATIVE_INFINITY;
        for (var m : merged) {
            if (m.angle - lastAngle < minGap) m.angle = lastAngle + minGap;
            lastAngle = m.angle;

            double dx = Math.cos(m.angle);
            double dy = Math.sin(m.angle);
            double maxX = W / 2.0 - 30;
            double maxY = H / 2.0 - 30;
            double scale = Math.min(Math.abs(maxX / dx), Math.abs(maxY / dy));
            int ex = (int) (cx + dx * scale);
            int ey = (int) (cy - dy * scale);

            drawIndicator(g, client, ex, ey, m.angle, m.count, m.intensity);
        }
    }

    private static double angleDiff(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }

    private record EdgeCandidate(double angle, float intensity, int mainColor) {}
    private static class MergedIndicator {
        double angle; int count; float intensity; int mainColor;
        MergedIndicator(double a, int c, float i, int col) {
            angle = a; count = c; intensity = i; mainColor = col;
        }
    }

    private static void drawIndicator(GuiGraphicsExtractor g, Minecraft client,
                                      int x, int y, double angle, int count, float intensity) {
        // 动态亮度
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.4 + 0.6);
        float brightness = intensity >= 0.95f ? 1.0f : Math.max(0.2f, pulse * intensity);
        int alpha = (int) (255 * brightness);
        int mainRed = (alpha << 24) | 0xFF0000;
        int innerRed = (alpha << 24) | 0xAA0000;
        int white = (alpha << 24) | 0xFFFFFF;

        // 半径：单个 10，多个合并后稍大
        int r = count > 1 ? 12 : 10;

        // 逐行水平线绘制圆形（每行 1 次 fill，比逐像素快 25 倍）
        for (int dy = -r; dy <= r; dy++) {
            int width = (int) Math.sqrt(r * r - dy * dy);
            g.fill(x - width, y + dy, x + width + 1, y + dy + 1, mainRed);
        }
        // 内圈（半径小 2）
        int r2 = r - 2;
        for (int dy = -r2; dy <= r2; dy++) {
            int width = (int) Math.sqrt(r2 * r2 - dy * dy);
            g.fill(x - width, y + dy, x + width + 1, y + dy + 1, innerRed);
        }

        if (count > 1) {
            // 合并多个：绘制 "×N" 数字
            String text = "×" + count;
            int w = client.font.width(text);
            g.text(client.font, text, x - w / 2, y - 4, white, false);
        } else {
            // 单个：绘制感叹号（2 次矩形代替逐像素）
            g.fill(x - 1, y - 5, x + 1, y + 2, white);
            g.fill(x - 1, y + 4, x + 1, y + 6, white);
        }

        // 指向箭头：用 1 次矩形 fill 代替 Bresenham 逐点循环
        int arrowLen = 8;
        int ax = x + (int) (Math.cos(angle) * (r + arrowLen));
        int ay = y - (int) (Math.sin(angle) * (r + arrowLen));
        drawThickLine(g, x + (int) (Math.cos(angle) * r), y - (int) (Math.sin(angle) * r), ax, ay, mainRed, 2);
    }

    // 用矩形填充近似粗线条：分解为水平/垂直/斜线三种情况
    private static void drawThickLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { g.fill(x0, y0, x0 + thickness, y0 + thickness, color); return; }
        // 采样 4 个点画粗矩形（代替每像素一次 fill）
        int half = thickness / 2;
        for (int i = 0; i <= steps; i += Math.max(1, steps / 4)) {
            int px = x0 + dx * i / steps;
            int py = y0 + dy * i / steps;
            g.fill(px - half, py - half, px + half + 1, py + half + 1, color);
        }
        g.fill(x1 - half, y1 - half, x1 + half + 1, y1 + half + 1, color);
    }
}