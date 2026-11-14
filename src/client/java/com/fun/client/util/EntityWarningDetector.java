package com.fun.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class EntityWarningDetector {
    public record WarningTarget(Entity entity, double distance, float intensity) {}

    private static final List<WarningTarget> targets = new ArrayList<>();

    public static void update() {
        targets.clear();
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !HudConfig.showEntityWarning) return;
        var p = client.player;

        var entities = client.level.getEntities(p, p.getBoundingBox().inflate(HudConfig.detectRange),
                EntityWarningDetector::isWarningEntity);

        for (Entity e : entities) {
            if (!e.isAlive()) continue;
            double dist = Math.sqrt(e.distanceToSqr(p));

            float intensity;
            if (dist <= HudConfig.dangerRange) {
                intensity = 1.0f;
            } else if (dist <= HudConfig.warningRange) {
                double rangeDiff = HudConfig.warningRange - HudConfig.dangerRange;
                if (rangeDiff <= 0) {
                    intensity = 1.0f;
                } else {
                    double t = (HudConfig.warningRange - dist) / rangeDiff;
                    intensity = (float) Math.min(1.0, Math.max(0.0, t));
                }
            } else {
                continue; // 超过警告范围，不加入
            }
            targets.add(new WarningTarget(e, dist, intensity));
        }

        // 按距离升序排序，最近的优先
        targets.sort((a, b) -> Double.compare(a.distance(), b.distance()));

        // 限制最大数量
        if (targets.size() > HudConfig.maxWarningEntities) {
            targets.subList(HudConfig.maxWarningEntities, targets.size()).clear();
        }
    }

    private static boolean isWarningEntity(Entity e) {
        if (e == Minecraft.getInstance().player) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && HudConfig.warningEntityTypes.contains(id.toString());
    }

    public static List<WarningTarget> getTargets() { return targets; }
    public static boolean hasThreat() { return !targets.isEmpty(); }
    public static void reset() { targets.clear(); }
}