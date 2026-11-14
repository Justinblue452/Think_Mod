package com.fun.client.render;

import com.fun.client.util.HudCache;
import com.fun.client.util.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class InfoHudRenderer {
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        if (client.player == null) return;

        int x = HudConfig.infoX;
        int y = client.getWindow().getGuiScaledHeight() - HudConfig.infoY;
        int bg = 0x80000000;
        int textColor = 0xFFA0E0FF;

        // 动态构建需要显示的行
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("think.hud.coord",
                (int)HudCache.posX, (int)HudCache.posY, (int)HudCache.posZ));
        lines.add(Component.translatable("think.hud.facing", HudCache.facing));
        lines.add(Component.translatable("think.hud.biome", HudCache.biome));

        if (HudCache.hasTarget) {
            lines.add(Component.translatable("think.hud.distance", String.format("%.0f", HudCache.distance)));
        } else {
            lines.add(Component.translatable("think.hud.distance.none"));
        }

        if (!HudCache.dimensionKey.isEmpty()) {
            lines.add(Component.translatable(HudCache.dimensionKey, HudCache.dimensionCoord));
        }

        // 渲染每一行
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int w = client.font.width(line) + 6;
            g.fill(x - 2, y + i * 11 - 1, x + w, y + i * 11 + 10, bg);
            g.text(client.font, line, x, y + i * 11, textColor, false);
        }
    }
}