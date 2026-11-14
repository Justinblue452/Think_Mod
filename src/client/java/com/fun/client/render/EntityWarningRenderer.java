package com.fun.client.render;

import com.fun.client.util.EntityWarningDetector;
import com.fun.client.util.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityWarningRenderer {
    private static final Map<Integer, SmoothPos> entityScreenCache = new HashMap<>();
    private static final List<SmoothPos> edgeScreenCache = new ArrayList<>();
    private static long lastSoundTime = 0;

    private static class SmoothPos {
        float x, y;
        SmoothPos(float x, float y) { this.x = x; this.y = y; }
    }

    // === 新增：清理缓存接口，供 ThinkClient 调用 ===
    public static void clearCache() {
        entityScreenCache.clear();
        edgeScreenCache.clear();
        lastSoundTime = 0;
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!HudConfig.hudVisible || !HudConfig.showEntityWarning) return;
        if (!EntityWarningDetector.hasThreat()) return;

        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || client.level == null) return;

        float partialTick = delta.getGameTimeDeltaTicks() % 1.0f;
        int W = client.getWindow().getGuiScaledWidth();
        int H = client.getWindow().getGuiScaledHeight();
        int cx = W / 2, cy = H / 2;

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 forward = player.getViewVector(partialTick);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double fov = client.options.fov().get();
        double tanHalfFovY = Math.tan(Math.toRadians(fov / 2.0));
        double tanHalfFovX = tanHalfFovY * ((double) W / H);

        float gameTime = client.level.getGameTime() + partialTick;
        float pulse = Mth.sin(gameTime * 0.15f) * 0.4f + 0.6f;

        List<EdgeCandidate> edgeCandidates = new ArrayList<>();
        var targets = EntityWarningDetector.getTargets();

        // 清理不在目标列表中的实体的平滑缓存
        if (!entityScreenCache.isEmpty()) {
            entityScreenCache.keySet().removeIf(id -> {
                for (var t : targets) if (t.entity().getId() == id) return false;
                return true;
            });
        }

        boolean playSound = false;

        for (var target : targets) {
            Entity entity = target.entity();
            float intensity = target.intensity();

            // 如果进入危险范围，且开启了音效，标记需要播放声音
            if (intensity >= 0.95f && HudConfig.soundEnabled) {
                playSound = true;
            }

            Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            Vec3 dir = center.subtract(eyePos).normalize();
            double x = dir.dot(right), y = dir.dot(up), z = dir.dot(forward);
            boolean inView = z > 0.1 && (Math.abs(x) < z * tanHalfFovX) && (Math.abs(y) < z * tanHalfFovY);

            float brightness = intensity >= 0.95f ? 1.0f : Math.max(0.2f, pulse * intensity);
            int alpha = (int) (255 * brightness);
            int mainRed = (alpha << 24) | 0xFF0000;
            int bgRed = ((int) (alpha * 0.4f) << 24) | 0xFF0000;

            if (inView) {
                float tx = (float) (cx + (x / z) / tanHalfFovX * cx);
                float ty = (float) (cy - (y / z) / tanHalfFovY * cy);
                SmoothPos sp = entityScreenCache.computeIfAbsent(entity.getId(), k -> new SmoothPos(tx, ty));
                float ddx = tx - sp.x, ddy = ty - sp.y;
                if (ddx * ddx + ddy * ddy > 1.0f) {
                    sp.x = Mth.lerp(0.25f, sp.x, tx);
                    sp.y = Mth.lerp(0.25f, sp.y, ty);
                }
                int boxW = 20, boxH = 30;
                int left = (int) (sp.x - boxW / 2.0), top = (int) (sp.y - boxH / 2.0);
                g.fill(left, top, left + boxW, top + boxH, bgRed);
                g.fill(left, top, left + boxW, top + 2, mainRed);
                g.fill(left, top + boxH - 2, left + boxW, top + boxH, mainRed);
                g.fill(left, top, left + 2, top + boxH, mainRed);
                g.fill(left + boxW - 2, top, left + boxW, top + boxH, mainRed);
            } else {
                double angle;
                if (z > 0) angle = Math.atan2(y, x);
                else {
                    if (Math.abs(x) > Math.abs(y)) angle = x > 0 ? 0 : Math.PI;
                    else angle = y > 0 ? Math.PI / 2 : -Math.PI / 2;
                    angle += Math.atan2(y, Math.abs(x)) * 0.2;
                }
                edgeCandidates.add(new EdgeCandidate(angle, intensity, mainRed));
            }
        }

        // 播放警告音效（1秒冷却）
        if (playSound) {
            long now = System.currentTimeMillis();
            if (now - lastSoundTime > 1000) {
                player.playSound(SoundEvents.CREEPER_PRIMED, 0.5f, 1.0f);
                lastSoundTime = now;
            }
        }

        // ... 边缘指示器合并与绘制逻辑保持不变 ...
        if (edgeCandidates.isEmpty()) return;

        edgeCandidates.sort(Comparator.comparingDouble(EdgeCandidate::angle));
        List<MergedIndicator> merged = new ArrayList<>();
        double mergeThreshold = Math.toRadians(12);
        for (var c : edgeCandidates) {
            boolean consumed = false;
            for (var m : merged) {
                if (Math.abs(angleDiff(m.angle, c.angle)) < mergeThreshold) {
                    m.count++; m.intensity = Math.max(m.intensity, c.intensity); consumed = true; break;
                }
            }
            if (!consumed) merged.add(new MergedIndicator(c.angle, 1, c.intensity));
        }

        double minGap = Math.toRadians(15);
        double lastAngle = Double.NEGATIVE_INFINITY;
        double maxX = W / 2.0 - 30, maxY = H / 2.0 - 30;

        while (edgeScreenCache.size() > merged.size()) edgeScreenCache.remove(edgeScreenCache.size() - 1);

        for (int i = 0; i < merged.size(); i++) {
            var m = merged.get(i);
            if (m.angle - lastAngle < minGap) m.angle = lastAngle + minGap;
            lastAngle = m.angle;

            double cosA = Math.cos(m.angle), sinA = Math.sin(m.angle);
            double scale = Math.min(Math.abs(maxX / cosA), Math.abs(maxY / sinA));
            float tx = (float) (cx + cosA * scale), ty = (float) (cy - sinA * scale);
            while (edgeScreenCache.size() <= i) edgeScreenCache.add(new SmoothPos(tx, ty));
            SmoothPos sp = edgeScreenCache.get(i);
            float ddx = tx - sp.x, ddy = ty - sp.y;
            if (ddx * ddx + ddy * ddy > 1.0f) {
                sp.x = Mth.lerp(0.3f, sp.x, tx);
                sp.y = Mth.lerp(0.3f, sp.y, ty);
            }
            drawIndicator(g, client, (int) sp.x, (int) sp.y, m.angle, m.count, m.intensity, pulse);
        }
    }

    // ... 下方 drawIndicator, angleDiff, drawThickLine 等方法保持不变 ...
    private static double angleDiff(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }
    private record EdgeCandidate(double angle, float intensity, int mainColor) {}
    private static class MergedIndicator {
        double angle; int count; float intensity;
        MergedIndicator(double a, int c, float i) { angle = a; count = c; intensity = i; }
    }
    private static void drawIndicator(GuiGraphicsExtractor g, Minecraft client, int x, int y, double angle, int count, float intensity, float pulse) {
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
        double cosA = Math.cos(angle), sinA = Math.sin(angle);
        drawThickLine(g, x + (int) (cosA * r), y - (int) (sinA * r), x + (int) (cosA * (r + 8)), y - (int) (sinA * (r + 8)), mainRed, 2);
    }
    private static void drawThickLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { g.fill(x0, y0, x0 + thickness, y0 + thickness, color); return; }
        int half = thickness / 2;
        int stepSize = Math.max(1, steps / 4);
        for (int i = 0; i <= steps; i += stepSize) {
            int px = x0 + dx * i / steps, py = y0 + dy * i / steps;
            g.fill(px - half, py - half, px + half + 1, py + half + 1, color);
        }
        g.fill(x1 - half, y1 - half, x1 + half + 1, y1 + half + 1, color);
    }
}