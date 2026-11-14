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
    private static final Set<EntityType<?>> cachedWarningTypes = new HashSet<>();
    private static int cachedTypeHash = 0;
    private static int tickCounter = 0;

    public static void update() {
        // 性能优化：每 2 tick 扫描一次（10Hz），人眼几乎无法感知延迟
        tickCounter++;
        if (tickCounter % 2 != 0) return;

        targets.clear();
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !HudConfig.showEntityWarning) return;

        refreshTypeCacheIfNeeded();
        if (cachedWarningTypes.isEmpty()) return;

        var p = client.player;
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

        // 排序 + 截断
        targets.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        while (targets.size() > HudConfig.maxWarningEntities) {
            targets.remove(targets.size() - 1);
        }
    }

    private static void refreshTypeCacheIfNeeded() {
        // 用整型 hashCode 代替字符串拼接，避免每 tick 的 GC 压力
        int currentHash = HudConfig.warningEntityTypes.hashCode();
        if (currentHash == cachedTypeHash) return;
        cachedTypeHash = currentHash;
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