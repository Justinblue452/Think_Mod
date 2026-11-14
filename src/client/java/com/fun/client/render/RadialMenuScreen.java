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
    private static final int RING_RADIUS = 110;
    private static final int BTN_W = 70;
    private static final int BTN_H = 26;

    private static final long ANIM_OPEN_MS = 150;
    private static final long ANIM_CLOSE_MS = 120;

    private long openTime = -1;
    private long closeTime = -1;
    private boolean closing = false;
    private float animProgress = 0f;

    private final Component[] labels;
    private final Component title;
    private final Component hint;
    private int hovered = -1;

    public RadialMenuScreen() {
        super(Component.translatable("think.radial.title"));
        this.labels = new Component[actions.length];
        for (int i = 0; i < actions.length; i++) {
            this.labels[i] = Component.translatable(actions[i].key);
        }
        this.title = Component.translatable("think.radial.title");
        this.hint = Component.translatable("think.radial.hint");
    }

    @Override
    protected void init() {
        super.init();
        this.openTime = System.currentTimeMillis();
        this.animProgress = 0f;
        this.closing = false;
        this.closeTime = -1;
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeTime = System.currentTimeMillis();
    }

    private void finalizeClose() {
        closing = false;
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        long now = System.currentTimeMillis();

        if (closing) {
            long elapsed = now - closeTime;
            float linear = Math.min(1.0f, elapsed / (float) ANIM_CLOSE_MS);
            animProgress = 1.0f - linear * linear;
            if (elapsed >= ANIM_CLOSE_MS) {
                finalizeClose();
                return;
            }
        } else {
            long elapsed = now - openTime;
            float linear = Math.min(1.0f, elapsed / (float) ANIM_OPEN_MS);
            animProgress = 1.0f - (float) Math.pow(1.0f - linear, 3);
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        hovered = (!closing && animProgress > 0.8f) ? getHovered(mouseX, mouseY, cx, cy) : -1;

        int bgAlpha = (int) (0x40 * animProgress);
        g.fill(0, 0, this.width, this.height, bgAlpha << 24);

        for (int i = 0; i < actions.length; i++) {
            double mid = (i + 0.5) * (2 * Math.PI / actions.length);
            double currentRadius = RING_RADIUS * animProgress;
            int bx = cx + (int) (Math.cos(mid) * currentRadius);
            int by = cy + (int) (Math.sin(mid) * currentRadius);

            int currentW = (int) (BTN_W * (0.5f + 0.5f * animProgress));
            int currentH = (int) (BTN_H * (0.5f + 0.5f * animProgress));

            int left = bx - currentW / 2;
            int top = by - currentH / 2;

            boolean isSel = (i == hovered);

            int baseAlpha = (int) (0x80 * animProgress);
            int selAlpha = (int) (0xDD * animProgress);
            int bgColor = isSel ? ((selAlpha << 24) | 0xFFFFFF) : ((baseAlpha << 24) | 0x202020);
            int textAlpha = (int) (255 * animProgress);
            int textColor = isSel ? ((textAlpha << 24) | 0x000000) : ((textAlpha << 24) | 0xFFFFFF);

            g.fill(left, top, left + currentW, top + currentH, bgColor);

            if (isSel) {
                int bAlpha = (int) (0xFF * animProgress);
                int borderColor = (bAlpha << 24) | 0xFF4444;
                g.fill(left, top, left + currentW, top + 2, borderColor);
                g.fill(left, top + currentH - 2, left + currentW, top + currentH, borderColor);
                g.fill(left, top, left + 2, top + currentH, borderColor);
                g.fill(left + currentW - 2, top, left + currentW, top + currentH, borderColor);
            }

            if (animProgress > 0.2f) {
                String num = String.valueOf(i + 1);
                g.text(this.font, num, left + 4, top + currentH - 12,
                        ((int) (255 * animProgress) << 24) | 0xFFAA00, true);

                Component label = this.labels[i];
                int w = this.font.width(label);
                g.text(this.font, label, bx - w / 2, top + (currentH - 8) / 2, textColor, false);
            }
        }

        if (animProgress > 0.5f) {
            int centerAlpha = (int) (255 * (animProgress - 0.5f) * 2);
            int color = (centerAlpha << 24) | 0xFFFFFF;
            int tw = this.font.width(title);
            g.text(this.font, title, cx - tw / 2, cy - 12, color, true);

            int hintColor = (centerAlpha << 24) | 0xAAAAAA;
            int hw = this.font.width(hint);
            g.text(this.font, hint, cx - hw / 2, cy + 2, hintColor, false);
        }
    }

    private int getHovered(double mx, double my, int cx, int cy) {
        for (int i = 0; i < actions.length; i++) {
            double mid = (i + 0.5) * (2 * Math.PI / actions.length);
            int bx = cx + (int) (Math.cos(mid) * RING_RADIUS);
            int by = cy + (int) (Math.sin(mid) * RING_RADIUS);
            int left = bx - BTN_W / 2;
            int top = by - BTN_H / 2;
            if (mx >= left && mx <= left + BTN_W && my >= top && my <= top + BTN_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing) return true;
        if (animProgress < 0.8f) return true;

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
        if (closing) return true;

        if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_V) {
            this.onClose();
            return true;
        }

        if (animProgress >= 0.8f) {
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
        }
        return super.keyPressed(event);
    }

    private void executeAction(Action action) {
        var client = this.minecraft;
        if (client == null || client.player == null || client.level == null) return;
        switch (action) {
            case LOCATOR -> { StrongholdLocator.startSession(); this.onClose(); }
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