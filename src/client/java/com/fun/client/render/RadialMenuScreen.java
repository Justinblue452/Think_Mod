package com.fun.client.render;

import com.fun.client.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public class RadialMenuScreen extends Screen {
    private enum Action {
        LOCATOR("think.radial.locator"),
        CANCEL_LOCATOR("think.radial.cancel_locator"),
        PORTAL("think.radial.portal"),
        COORD("think.radial.coord"),
        HUD_EDITOR("think.radial.hud_editor"),
        HUD_TOGGLE("think.radial.hud_toggle"),
        CONFIG("think.radial.config"),
        RESET_ALL("think.radial.reset_all");

        public final String key;
        Action(String key) { this.key = key; }
    }

    private final Action[] actions = Action.values();
    private static final int INNER_RADIUS = 50;
    private static final int OUTER_RADIUS = 110;
    private int hovered = -1;

    public RadialMenuScreen() {
        super(Component.translatable("think.radial.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int cy = this.height / 2;
        hovered = getHovered(mouseX, mouseY, cx, cy);

        g.fill(0, 0, this.width, this.height, 0x40000000);

        int steps = 60;
        for (int i = 0; i < actions.length; i++) {
            boolean sel = (i == hovered);
            int color = sel ? 0xCCFFFFFF : 0x80202020;
            int textColor = sel ? 0xFF000000 : 0xFFFFFFFF;

            double a0 = i * (2 * Math.PI / actions.length);
            double a1 = (i + 1) * (2 * Math.PI / actions.length);
            for (int s = 0; s <= steps; s++) {
                double angle = a0 + (a1 - a0) * (s / (double) steps);
                double cosA = Math.cos(angle), sinA = Math.sin(angle);
                for (int r = INNER_RADIUS; r <= OUTER_RADIUS; r += 2) {
                    int px = cx + (int)(cosA * r);
                    int py = cy + (int)(sinA * r);
                    g.fill(px, py, px + 2, py + 2, color);
                }
            }

            double mid = (a0 + a1) / 2;
            double tr = (INNER_RADIUS + OUTER_RADIUS) / 2.0;
            int tx = cx + (int)(Math.cos(mid) * tr);
            int ty = cy + (int)(Math.sin(mid) * tr);

            // 数字索引（方便键盘玩家）
            String num = String.valueOf(i + 1);
            int nw = this.font.width(num);
            g.text(this.font, num, tx - nw / 2, ty - 12, 0xFFFFAA00, true);

            // 标签
            Component label = Component.translatable(actions[i].key);
            int w = this.font.width(label);
            g.text(this.font, label, tx - w / 2, ty + 2, textColor, false);
        }

        Component hint = Component.translatable("think.radial.hint");
        int hw = this.font.width(hint);
        g.text(this.font, hint, cx - hw / 2, cy - 4, 0xFFFFFFFF, false);
    }

    private int getHovered(double mx, double my, int cx, int cy) {
        double dx = mx - cx, dy = my - cy;
        double d2 = dx*dx + dy*dy;
        if (d2 > INNER_RADIUS*INNER_RADIUS && d2 < OUTER_RADIUS*OUTER_RADIUS*1.5) {
            double a = Math.atan2(dy, dx);
            if (a < 0) a += 2*Math.PI;
            return (int) Math.floor(a / (2*Math.PI / actions.length));
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int cx = this.width / 2, cy = this.height / 2;
        int idx = getHovered(event.x(), event.y(), cx, cy);
        if (idx >= 0 && idx < actions.length) {
            var player = this.minecraft != null ? this.minecraft.player : null;
            if (player != null) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
            executeAction(actions[idx]);
            return true;
        }
        this.onClose();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) { this.onClose(); return true; }
        // 数字键 1-8 直接选择对应扇区
        int key = event.key();
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_8) {
            int idx = key - InputConstants.KEY_1;
            if (idx < actions.length) {
                var player = this.minecraft != null ? this.minecraft.player : null;
                if (player != null) player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
                executeAction(actions[idx]);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void executeAction(Action action) {
        var client = this.minecraft;
        if (client == null || client.player == null || client.level == null) return;
        switch (action) {
            case LOCATOR -> {
                StrongholdLocator.startSession();
                this.onClose();
            }
            case CANCEL_LOCATOR -> {
                if (StrongholdLocator.getState() != StrongholdLocator.SessionState.IDLE) {
                    StrongholdLocator.reset();
                    client.player.sendOverlayMessage(Component.translatable("think.locate.cancelled"));
                } else {
                    client.player.sendOverlayMessage(Component.translatable("think.locate.none"));
                }
                this.onClose();
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
            case HUD_EDITOR -> client.gui.setScreen(new HudEditorScreen());
            case HUD_TOGGLE -> {
                HudConfig.hudVisible = !HudConfig.hudVisible;
                client.player.sendOverlayMessage(Component.translatable("think.command.hud",
                        HudConfig.hudVisible ? Component.translatable("think.state.on") : Component.translatable("think.state.off")));
                ConfigManager.save();
                this.onClose();
            }
            case CONFIG -> client.gui.setScreen(new ThinkConfigScreen(null));
            case RESET_ALL -> {
                StrongholdLocator.reset();
                PortalAssistant.clearTarget();
                ConfigManager.save();
                client.player.sendOverlayMessage(Component.translatable("think.command.reset_all"));
                this.onClose();
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}