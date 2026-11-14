package com.fun.client.render;

import com.fun.client.util.HudConfig;
import com.fun.client.util.StrongholdLocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class SessionHudRenderer {
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!HudConfig.hudVisible) return;
        var state = StrongholdLocator.getState();
        if (state == StrongholdLocator.SessionState.IDLE
                || state == StrongholdLocator.SessionState.DONE) return;

        var client = Minecraft.getInstance();
        if (client.player == null) return;

        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int y = 20;

        Component text;
        int color;
        if (state == StrongholdLocator.SessionState.WAITING_FIRST) {
            text = Component.translatable("think.session.waiting_first");
            color = 0xFFFFAA00;
        } else { // WAITING_SECOND
            double dist = StrongholdLocator.getDistanceFromFirst();
            if (dist < 20.0) {
                text = Component.translatable("think.session.move_more",
                        (int) Math.ceil(20.0 - dist));
                color = 0xFFFFAA00;
            } else {
                text = Component.translatable("think.session.ready");
                color = 0xFF55FF55;
            }
        }

        int w = client.font.width(text);
        int bg = 0x80000000;
        g.fill(cx - w / 2 - 6, y - 3, cx + w / 2 + 6, y + 11, bg);
        g.text(client.font, text, cx - w / 2, y, color, false);
    }
}