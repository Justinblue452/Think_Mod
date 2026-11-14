package com.fun.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityWarningDetector {
    public record WarningTarget(Entity entity, double distance, float intensity) {}

    private static final List<WarningTarget> targets = new ArrayList<>();
    // 缓存解析好的 EntityType，热路径不再做字符串比较
    private static final Set<EntityType<?>> cachedWarningTypes = new HashSet<>();
    private static String cachedConfigHash = "";

    public static void update() {
        targets.clear();
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !HudConfig.showEntityWarning) return;

        refreshTypeCacheIfNeeded();
        if (cachedWarningTypes.isEmpty()) return;

        var p = client.player;
        // 使用缓存的 EntityType 集合做快速过滤
        var entities = client.level.getEntities(p, p.getBoundingBox().inflate(HudConfig.detectRange),
                e -> e != p && cachedWarningTypes.contains(e.getType()));

        for (Entity e : entities) {
            double dist = Math.sqrt(e.distanceToSqr(p));
            float intensity;
            if (dist <= HudConfig.dangerRange) {
                intensity = 1.0f;
            } else if (dist <= HudConfig.warningRange) {
                double rangeDiff = HudConfig.warningRange - HudConfig.dangerRange;
                if (rangeDiff <= 0) intensity = 1.0f;
                else {
                    double t = (HudConfig.warningRange - dist) / rangeDiff;
                    intensity = (float) Math.min(1.0, Math.max(0.0, t));
                }
            } else {
                continue;
            }
            targets.add(new WarningTarget(e, dist, intensity));
        }

        targets.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        if (targets.size() > HudConfig.maxWarningEntities) {
            targets.subList(HudConfig.maxWarningEntities, targets.size()).clear();
        }
    }

    private static void refreshTypeCacheIfNeeded() {
        // 用简单字符串哈希检测配置变更，避免每次 tick 重建
        String hash = String.join(",", HudConfig.warningEntityTypes);
        if (hash.equals(cachedConfigHash)) return;
        cachedConfigHash = hash;
        cachedWarningTypes.clear();
        for (String id : HudConfig.warningEntityTypes) {
            Identifier rl = Identifier.tryParse(id);
            if (rl == null) continue;
            BuiltInRegistries.ENTITY_TYPE.getOptional(rl).ifPresent(cachedWarningTypes::add);
        }
    }

    public static List<WarningTarget> getTargets() { return targets; }
    public static boolean hasThreat() { return !targets.isEmpty(); }
    public static void reset() { targets.clear(); }
}