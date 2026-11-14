package com.fun.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class HudCache {
    public static double posX, posY, posZ;
    public static String facing = "";
    public static String biome = "";

    public static boolean hasTarget = false;
    public static double distance = 0;
    public static float arrowAngle = 0;

    public static String dimensionKey = ""; // 翻译键，如 "think.hud.nether"
    public static String dimensionCoord = ""; // 格式化后的坐标字符串，如 "0, 10, 0"

    public static void update() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        var p = client.player;
        BlockPos pos = p.blockPosition();

        posX = pos.getX();
        posY = pos.getY();
        posZ = pos.getZ();
        facing = CoordinateUtil.getFacing(p);

        // 获取生物群系 ID (兼容 1.21.6+)
        var biomeHolder = client.level.getBiome(pos);
        Identifier biomeId = client.level.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .getKey(biomeHolder.value());
        biome = (biomeId != null) ? biomeId.getPath() : "unknown";

        // 三角定位目标与距离
        hasTarget = StrongholdLocator.hasValidTarget();
        if (hasTarget) {
            double dx = StrongholdLocator.getTargetX() - p.getX();
            double dz = StrongholdLocator.getTargetZ() - p.getZ();
            distance = Math.sqrt(dx * dx + dz * dz);

            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            arrowAngle = (float) (targetYaw - p.getYRot());
        } else {
            distance = 0;
            arrowAngle = 0;
        }

        // 维度对照
        if (client.level.dimension() == Level.OVERWORLD) {
            var n = CoordinateUtil.overworldToNether(pos);
            dimensionKey = "think.hud.nether";
            dimensionCoord = String.format("%d, %d, %d", n.getX(), n.getY(), n.getZ());
        } else if (client.level.dimension() == Level.NETHER) {
            var o = CoordinateUtil.netherToOverworld(pos);
            dimensionKey = "think.hud.overworld";
            dimensionCoord = String.format("%d, %d, %d", o.getX(), o.getY(), o.getZ());
        } else {
            dimensionKey = "";
            dimensionCoord = "";
        }
    }
}