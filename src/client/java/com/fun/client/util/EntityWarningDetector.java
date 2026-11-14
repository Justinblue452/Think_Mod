package com.fun.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public class EntityWarningDetector {
    private static double nearestDistance = Double.MAX_VALUE;
    private static float warningIntensity = 0f;
    private static Entity nearestEntity = null;

    public static void update() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !HudConfig.showEntityWarning) {
            reset();
            return;
        }
        var p = client.player;

        var entities = client.level.getEntities(p, p.getBoundingBox().inflate(HudConfig.detectRange),
                EntityWarningDetector::isWarningEntity);

        double nearestSq = Double.MAX_VALUE;
        nearestEntity = null;
        for (Entity e : entities) {
            if (!e.isAlive()) continue;
            double d = e.distanceToSqr(p);
            if (d < nearestSq) {
                nearestSq = d;
                nearestEntity = e;
            }
        }
        nearestDistance = Math.sqrt(nearestSq);

        if (nearestEntity != null && nearestDistance <= HudConfig.dangerRange) {
            warningIntensity = 1.0f;
        } else if (nearestEntity != null && nearestDistance <= HudConfig.warningRange) {
            double rangeDiff = HudConfig.warningRange - HudConfig.dangerRange;
            if (rangeDiff <= 0) {
                warningIntensity = 1.0f;
            } else {
                double t = (HudConfig.warningRange - nearestDistance) / rangeDiff;
                warningIntensity = (float) Math.min(1.0, Math.max(0.0, t));
            }
        } else {
            warningIntensity = 0f;
        }
    }

    private static boolean isWarningEntity(Entity e) {
        if (e == Minecraft.getInstance().player) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && HudConfig.warningEntityTypes.contains(id.toString());
    }

    public static void reset() {
        nearestDistance = Double.MAX_VALUE;
        warningIntensity = 0f;
        nearestEntity = null;
    }

    public static boolean hasThreat() { return warningIntensity > 0f && nearestEntity != null; }
    public static float getWarningIntensity() { return warningIntensity; }
    public static Entity getNearestEntity() { return nearestEntity; }
}