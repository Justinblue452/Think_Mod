package com.fun.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class CoordinateUtil {
    public static final float DEFAULT_TPS = 20.0f;

    public static BlockPos overworldToNether(BlockPos pos) {
        return new BlockPos((int) Math.floor(pos.getX() / 8.0), pos.getY(), (int) Math.floor(pos.getZ() / 8.0));
    }

    public static BlockPos netherToOverworld(BlockPos pos) {
        return new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
    }

    public static float ticksToSeconds(int ticks) { return ticks / DEFAULT_TPS; }
    public static int secondsToTicks(float seconds) { return Math.round(seconds * DEFAULT_TPS); }

    public static String getFacing(Player player) {
        float yaw = ((player.getYRot() % 360) + 360) % 360;
        String dir;
        if (yaw < 45 || yaw >= 315) dir = "S";
        else if (yaw < 135) dir = "W";
        else if (yaw < 225) dir = "N";
        else dir = "E";
        return String.format("%s (%.0f°)", dir, yaw);
    }
}