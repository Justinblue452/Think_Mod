package com.fun.client.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StrongholdLocator {
    public record ThrowRay(double x, double z, double dx, double dz) {}
    public enum SessionState { IDLE, WAITING_FIRST, WAITING_SECOND, DONE }

    private static final List<ThrowRay> rays = new ArrayList<>();
    private static final Set<Integer> processedEyeIds = new HashSet<>();
    private static SessionState state = SessionState.IDLE;
    private static ThrowRay firstRay = null;
    private static double targetX = Double.NaN, targetZ = Double.NaN;
    private static Level lastLevel = null;

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());
    }

    /** 由 L 键触发，开始新的定位会话 */
    public static void startSession() {
        reset();
        var client = Minecraft.getInstance();
        // 屏蔽当前已存在的眼睛，只记录会话开始后新投出的
        if (client.level != null && client.player != null) {
            var existing = client.level.getEntitiesOfClass(
                    EyeOfEnder.class, client.player.getBoundingBox().inflate(128.0));
            for (EyeOfEnder eye : existing) processedEyeIds.add(eye.getId());
        }
        state = SessionState.WAITING_FIRST;
        sendMsg(Component.translatable("think.locate.start"));
    }

    public static void onClientTick() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        // 维度变化时自动重置，防止数据污染
        if (lastLevel != null && lastLevel != client.level) reset();
        lastLevel = client.level;

        if (state != SessionState.WAITING_FIRST && state != SessionState.WAITING_SECOND) return;

        var eyes = client.level.getEntitiesOfClass(
                EyeOfEnder.class, client.player.getBoundingBox().inflate(128.0));

        for (EyeOfEnder eye : eyes) {
            if (processedEyeIds.contains(eye.getId())) continue;
            if (eye.tickCount < 2) continue;

            Vec3 vel = eye.getDeltaMovement();
            double dx = vel.x, dz = vel.z;
            if (dx * dx + dz * dz < 0.01) continue;

            double len = Math.sqrt(dx * dx + dz * dz);
            dx /= len; dz /= len;

            double px = eye.getX(), pz = eye.getZ();

            if (state == SessionState.WAITING_FIRST) {
                firstRay = new ThrowRay(px, pz, dx, dz);
                processedEyeIds.add(eye.getId());
                state = SessionState.WAITING_SECOND;
                sendMsg(Component.translatable("think.locate.first_recorded"));
            } else { // WAITING_SECOND
                double dist = Math.sqrt(
                        (firstRay.x() - px) * (firstRay.x() - px) +
                                (firstRay.z() - pz) * (firstRay.z() - pz));
                if (dist < 20.0) {
                    sendMsg(Component.translatable("think.locate.too_close", (int) dist));
                    processedEyeIds.add(eye.getId());
                    continue;
                }
                double dot = Math.abs(firstRay.dx() * dx + firstRay.dz() * dz);
                if (dot > 0.966) { // 夹角 < 15°
                    sendMsg(Component.translatable("think.locate.parallel"));
                    processedEyeIds.add(eye.getId());
                    continue;
                }
                rays.clear();
                rays.add(firstRay);
                rays.add(new ThrowRay(px, pz, dx, dz));
                computeIntersection();
                processedEyeIds.add(eye.getId());

                if (!Double.isNaN(targetX)) {
                    state = SessionState.DONE;
                    sendMsg(Component.translatable("think.locate.done",
                            String.format("%.1f", targetX), String.format("%.1f", targetZ)));
                } else {
                    sendMsg(Component.translatable("think.locate.failed"));
                    state = SessionState.WAITING_SECOND;
                    rays.clear();
                }
            }
            break;
        }
    }

    private static void computeIntersection() {
        if (rays.size() < 2) return;
        ThrowRay r1 = rays.get(0), r2 = rays.get(1);
        double det = r1.dx() * (-r2.dz()) - r1.dz() * (-r2.dx());
        if (Math.abs(det) < 1e-6) { targetX = targetZ = Double.NaN; return; }
        double bx = r2.x() - r1.x(), bz = r2.z() - r1.z();
        double t1 = (bx * (-r2.dz()) - bz * (-r2.dx())) / det;
        if (t1 <= 0) { targetX = targetZ = Double.NaN; return; }
        targetX = r1.x() + t1 * r1.dx();
        targetZ = r1.z() + t1 * r1.dz();
    }

    public static SessionState getState() { return state; }

    public static double getDistanceFromFirst() {
        if (firstRay == null) return 0;
        var p = Minecraft.getInstance().player;
        if (p == null) return 0;
        double dx = firstRay.x() - p.getX();
        double dz = firstRay.z() - p.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean hasValidTarget() { return !Double.isNaN(targetX); }
    public static double getTargetX() { return targetX; }
    public static double getTargetZ() { return targetZ; }
    public static int getRayCount() { return rays.size(); }
    public static int getLastEyeCount() { return processedEyeIds.size(); }

    public static void reset() {
        rays.clear();
        processedEyeIds.clear();
        firstRay = null;
        targetX = targetZ = Double.NaN;
        state = SessionState.IDLE;
    }

    private static void sendMsg(Component msg) {
        var p = Minecraft.getInstance().player;
        if (p != null) {
            // 1.21.11+ 中 displayClientMessage(msg, false) 的替代方法
            p.sendSystemMessage(msg);
        }
    }
}