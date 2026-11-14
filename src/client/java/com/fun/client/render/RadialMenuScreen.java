package com.fun.client.render;

import com.fun.client.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class RadialMenuScreen extends Screen {
    private enum Action {
        LOCATOR("think.radial.locator"),
        PORTAL("think.radial.portal"),
        COORD("think.radial.coord"),
        HUD_EDITOR("think.radial.hud_editor"),
        HUD_TOGGLE("think.radial.hud_toggle"),
        CONFIG("think.radial.config");

        public final String key;
        Action(String key) { this.key = key; }
    }

    private final Action[] actions = Action.values();
    private int selectedIndex = -1;

    private static final int INNER_RADIUS = 40;
    private static final int OUTER_RADIUS = 90;

    public RadialMenuScreen() {
        super(Component.translatable("think.radial.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;

        // 实时计算鼠标悬停的扇区
        selectedIndex = getHoveredIndex(mouseX, mouseY, cx, cy);

        g.fill(0, 0, this.width, this.height, 0x40000000);

        int steps = 40;
        for (int i = 0; i < actions.length; i++) {
            boolean isSelected = (i == selectedIndex);
            int color = isSelected ? 0xCCFFFFFF : 0x80202020;
            int textColor = isSelected ? 0xFF000000 : 0xFFFFFFFF;

            double startAngle = i * (2 * Math.PI / actions.length);
            double endAngle = (i + 1) * (2 * Math.PI / actions.length);

            for (int s = 0; s <= steps; s++) {
                double angle = startAngle + (endAngle - startAngle) * (s / (double) steps);
                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);
                for (int r = INNER_RADIUS; r <= OUTER_RADIUS; r++) {
                    int px = cx + (int) (cosA * r);
                    int py = cy + (int) (sinA * r);
                    g.fill(px, py, px + 2, py + 2, color);
                }
            }

            double midAngle = (startAngle + endAngle) / 2;
            double textRadius = (INNER_RADIUS + OUTER_RADIUS) / 2.0;
            int tx = cx + (int) (Math.cos(midAngle) * textRadius);
            int ty = cy + (int) (Math.sin(midAngle) * textRadius);

            Component label = Component.translatable(actions[i].key);
            int w = this.font.width(label);
            g.text(this.font, label, tx - w / 2, ty - 4, textColor, false);
        }

        Component hint = Component.translatable("think.radial.hint");
        int hw = this.font.width(hint);
        g.text(this.font, hint, cx - hw / 2, cy - 4, 0xFFFFFFFF, false);
    }

    // 计算鼠标所在扇区
    private int getHoveredIndex(double mouseX, double mouseY, int cx, int cy) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distSq = dx * dx + dy * dy;

        // 适当放大外圈点击容差 (OUTER_RADIUS * 1.5)
        if (distSq > INNER_RADIUS * INNER_RADIUS && distSq < OUTER_RADIUS * OUTER_RADIUS * 1.5) {
            double angle = Math.atan2(dy, dx);
            if (angle < 0) angle += 2 * Math.PI;
            return (int) Math.floor(angle / (2 * Math.PI / actions.length));
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 仅处理左键
        if (event.button() == 0) {
            int cx = this.width / 2;
            int cy = this.height / 2;

            // 核心修复：在点击的瞬间重新计算一次坐标，防止丢帧导致选中失败
            int clickedIndex = getHoveredIndex(event.x(), event.y(), cx, cy);

            if (clickedIndex >= 0 && clickedIndex < actions.length) {
                executeAction(actions[clickedIndex]);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_V) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void executeAction(Action action) {
        var client = this.minecraft;
        if (client == null || client.player == null || client.level == null) return;

        // 核心修复：只处理一次界面切换
        switch (action) {
            case LOCATOR -> {
                StrongholdLocator.startSession();
                this.onClose(); // 回到游戏
            }
            case PORTAL -> {
                if (PortalAssistant.hasTarget()) {
                    PortalAssistant.clearTarget();
                    client.player.sendOverlayMessage(Component.translatable("think.portal.cleared"));
                } else {
                    var pos = client.player.blockPosition();
                    String dim = "minecraft:overworld";
                    if (client.level.dimension() == Level.NETHER) dim = "minecraft:the_nether";
                    else if (client.level.dimension() == Level.END) dim = "minecraft:the_end";
                    PortalAssistant.setTarget(pos.getX(), pos.getY(), pos.getZ(), dim);
                    client.player.sendOverlayMessage(Component.translatable("think.portal.set", pos.getX(), pos.getZ()));
                }
                ConfigManager.save();
                this.onClose();
            }
            case COORD -> {
                var pos = client.player.blockPosition();
                if (client.level.dimension() == Level.OVERWORLD) {
                    var n = CoordinateUtil.overworldToNether(pos);
                    client.player.sendOverlayMessage(Component.translatable("think.portal.toNether", n.getX(), n.getY(), n.getZ()));
                } else if (client.level.dimension() == Level.NETHER) {
                    var o = CoordinateUtil.netherToOverworld(pos);
                    client.player.sendOverlayMessage(Component.translatable("think.portal.toOverworld", o.getX(), o.getY(), o.getZ()));
                } else {
                    client.player.sendOverlayMessage(Component.translatable("think.portal.wrongDim"));
                }
                this.onClose();
            }
            case HUD_EDITOR -> {
                // 跳转到编辑器（不需要调用 onClose）
                client.gui.setScreen(new HudEditorScreen());
            }
            case HUD_TOGGLE -> {
                HudConfig.hudVisible = !HudConfig.hudVisible;
                client.player.sendOverlayMessage(Component.translatable("think.command.hud",
                        HudConfig.hudVisible ? Component.translatable("think.state.on") : Component.translatable("think.state.off")));
                ConfigManager.save();
                this.onClose();
            }
            case CONFIG -> {
                // 跳转到设置（不需要调用 onClose）
                client.gui.setScreen(new ThinkConfigScreen(null));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}