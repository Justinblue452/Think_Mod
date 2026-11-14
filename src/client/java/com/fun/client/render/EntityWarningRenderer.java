package com.fun.client.render;

import com.fun.client.util.EntityWarningDetector;
import com.fun.client.util.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityWarningRenderer {

    // === 防抖缓存 ===
    // 视野内实体的屏幕坐标平滑缓存 (Key: EntityId)
    private static final Map<Integer, SmoothPos> entityScreenCache = new HashMap<>();
    // 边缘指示器的屏幕坐标平滑缓存 (基于排序后的索引)
    private static final List<SmoothPos> edgeScreenCache = new ArrayList<>();

    private static class SmoothPos {
        float x, y;
        SmoothPos(float x, float y) { this.x = x; this.y = y; }
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!HudConfig.hudVisible || !HudConfig.showEntityWarning) return;
        if (!EntityWarningDetector.hasThreat()) return;

        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || client.level == null) return;

        // 获取平滑时间
        float partialTick = delta.getGameTimeDeltaTicks() % 1.0f;

        int W = client.getWindow().getGuiScaledWidth();
        int H = client.getWindow().getGuiScaledHeight();
        int cx = W / 2;
        int cy = H / 2;

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 forward = player.getViewVector(partialTick);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double fov = client.options.fov().get();
        double aspectRatio = (double) W / H;
        double tanHalfFovY = Math.tan(Math.toRadians(fov / 2.0));
        double tanHalfFovX = tanHalfFovY * aspectRatio;

        float gameTime = client.level.getGameTime() + partialTick;
        float pulse = Mth.sin(gameTime * 0.15f) * 0.4f + 0.6f;

        List<EdgeCandidate> edgeCandidates = new ArrayList<>();
        var targets = EntityWarningDetector.getTargets();

        // 清理已经不在目标列表中的实体平滑缓存，防止内存泄漏（目标极少，开销极低）
        if (!entityScreenCache.isEmpty()) {
            entityScreenCache.keySet().removeIf(id -> {
                for (var t : targets) if (t.entity().getId() == id) return false;
                return true;
            });
        }

        for (var target : targets) {
            Entity entity = target.entity();
            float intensity = target.intensity();

            // 保留 3D 速度预判
            Vec3 rawPos = entity.position();
            Vec3 predicted = rawPos.add(entity.getDeltaMovement().scale(partialTick));
            Vec3 center = predicted.add(0, entity.getBbHeight() * 0.5, 0);

            Vec3 dir = center.subtract(eyePos).normalize();

            double x = dir.dot(right);
            double y = dir.dot(up);
            double z = dir.dot(forward);

            boolean inView = z > 0.1 && (Math.abs(x) < z * tanHalfFovX) && (Math.abs(y) < z * tanHalfFovY);

            float brightness = intensity >= 0.95f ? 1.0f : Math.max(0.2f, pulse * intensity);
            int alpha = (int) (255 * brightness);
            int mainRed = (alpha << 24) | 0xFF0000;
            int bgRed = ((int) (alpha * 0.4f) << 24) | 0xFF0000;

            if (inView) {
                // 计算原始屏幕坐标
                float tx = (float) (cx + (x / z) / tanHalfFovX * cx);
                float ty = (float) (cy - (y / z) / tanHalfFovY * cy);

                // === 防抖：屏幕坐标平滑 ===
                SmoothPos sp = entityScreenCache.computeIfAbsent(entity.getId(), k -> new SmoothPos(tx, ty));
                float dx = tx - sp.x;
                float dy = ty - sp.y;
                // 如果移动幅度大于 1 像素，才进行平滑跟随，否则保持静止（死区）
                if (dx * dx + dy * dy > 1.0f) {
                    sp.x = Mth.lerp(0.25f, sp.x, tx);
                    sp.y = Mth.lerp(0.25f, sp.y, ty);
                }

                int boxW = 20, boxH = 30;
                int left = (int) (sp.x - boxW / 2.0);
                int top = (int) (sp.y - boxH / 2.0);

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

        // 角度分组合并
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
            if (!consumed) merged.add(new MergedIndicator(c.angle, 1, c.intensity));
        }

        // 拉开重叠
        double minGap = Math.toRadians(15);
        double lastAngle = Double.NEGATIVE_INFINITY;
        double maxX = W / 2.0 - 30;
        double maxY = H / 2.0 - 30;

        // 清除多余的边缘缓存（如果合并后的指示器数量变少）
        while (edgeScreenCache.size() > merged.size()) {
            edgeScreenCache.remove(edgeScreenCache.size() - 1);
        }

        for (int i = 0; i < merged.size(); i++) {
            var m = merged.get(i);
            if (m.angle - lastAngle < minGap) m.angle = lastAngle + minGap;
            lastAngle = m.angle;

            double cosA = Math.cos(m.angle);
            double sinA = Math.sin(m.angle);
            double scale = Math.min(Math.abs(maxX / cosA), Math.abs(maxY / sinA));
            float tx = (float) (cx + cosA * scale);
            float ty = (float) (cy - sinA * scale);

            // === 边缘指示器坐标平滑 ===
            while (edgeScreenCache.size() <= i) {
                edgeScreenCache.add(new SmoothPos(tx, ty));
            }
            SmoothPos sp = edgeScreenCache.get(i);
            float dx = tx - sp.x;
            float dy = ty - sp.y;
            if (dx * dx + dy * dy > 1.0f) {
                sp.x = Mth.lerp(0.3f, sp.x, tx);
                sp.y = Mth.lerp(0.3f, sp.y, ty);
            }

            drawIndicator(g, client, (int) sp.x, (int) sp.y, m.angle, m.count, m.intensity, pulse);
        }
    }

    private static double angleDiff(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }

    private record EdgeCandidate(double angle, float intensity, int mainColor) {}
    private static class MergedIndicator {
        double angle; int count; float intensity;
        MergedIndicator(double a, int c, float i) { angle = a; count = c; intensity = i; }
    }

    private static void drawIndicator(GuiGraphicsExtractor g, Minecraft client,
                                      int x, int y, double angle, int count, float intensity, float pulse) {
        float brightness = intensity >= 0.95f ? 1.0f : Math.max(0.2f, pulse * intensity);
        int alpha = (int) (255 * brightness);
        int mainRed = (alpha << 24) | 0xFF0000;
        int innerRed = (alpha << 24) | 0xAA0000;
        int white = (alpha << 24) | 0xFFFFFF;

        int r = count > 1 ? 12 : 10;

        for (int dy = -r; dy <= r; dy++) {
            int width = (int) Math.sqrt(r * r - dy * dy);
            g.fill(x - width, y + dy, x + width + 1, y + dy + 1, mainRed);
        }
        int r2 = r - 2;
        for (int dy = -r2; dy <= r2; dy++) {
            int width = (int) Math.sqrt(r2 * r2 - dy * dy);
            g.fill(x - width, y + dy, x + width + 1, y + dy + 1, innerRed);
        }

        if (count > 1) {
            String text = "×" + count;
            int w = client.font.width(text);
            g.text(client.font, text, x - w / 2, y - 4, white, false);
        } else {
            g.fill(x - 1, y - 5, x + 1, y + 2, white);
            g.fill(x - 1, y + 4, x + 1, y + 6, white);
        }

        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        int sx = x + (int) (cosA * r);
        int sy = y - (int) (sinA * r);
        int tx = x + (int) (cosA * (r + 8));
        int ty = y - (int) (sinA * (r + 8));
        drawThickLine(g, sx, sy, tx, ty, mainRed, 2);
    }

    private static void drawThickLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { g.fill(x0, y0, x0 + thickness, y0 + thickness, color); return; }
        int half = thickness / 2;
        int stepSize = Math.max(1, steps / 4);
        for (int i = 0; i <= steps; i += stepSize) {
            int px = x0 + dx * i / steps;
            int py = y0 + dy * i / steps;
            g.fill(px - half, py - half, px + half + 1, py + half + 1, color);
        }
        g.fill(x1 - half, y1 - half, x1 + half + 1, y1 + half + 1, color);
    }
}