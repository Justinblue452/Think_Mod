package com.fun.client.render;

import com.fun.client.util.ConfigManager;
import com.fun.client.util.HudConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private boolean draggingArrow = false;
    private double startMouseX, startMouseY;
    private int startArrowX, startArrowY;

    public HudEditorScreen() {
        super(Component.translatable("think.screen.title"));
    }

    @Override
    protected void init() {
        super.init(); // 必须调用
        // 在屏幕底部增加一个前往设置界面的按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("think.config.title"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new ThinkConfigScreen(this));
                    }
                }
        ).bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        var client = this.minecraft;
        if (client == null || client.player == null) return;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        g.fill(0, 0, screenW, screenH, 0x60000000);

        int ax = screenW / 2 + HudConfig.arrowX;
        int ay = screenH - HudConfig.arrowY;
        g.fill(ax - 20, ay - 20, ax + 20, ay + 20, draggingArrow ? 0x80FFFF00 : 0x40FFFFFF);
        g.text(client.font, Component.translatable("think.screen.arrow"), ax - 12, ay - 5, 0xFFFFFFFF, false);

        g.text(client.font, Component.translatable("think.screen.hint"), screenW / 2 - 100, 20, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 先让按钮处理点击
        if (super.mouseClicked(event, doubleClick)) return true;

        if (event.button() != 0) return false;
        var client = this.minecraft;
        if (client == null) return false;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        double mouseX = event.x();
        double mouseY = event.y();

        int ax = screenW / 2 + HudConfig.arrowX;
        int ay = screenH - HudConfig.arrowY;
        if (Math.abs(mouseX - ax) < 25 && Math.abs(mouseY - ay) < 25) {
            draggingArrow = true;
            startMouseX = mouseX;
            startMouseY = mouseY;
            startArrowX = HudConfig.arrowX;
            startArrowY = HudConfig.arrowY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingArrow) {
            double currentMouseX = event.x();
            double currentMouseY = event.y();
            HudConfig.arrowX = startArrowX + (int) (currentMouseX - startMouseX);
            HudConfig.arrowY = startArrowY - (int) (currentMouseY - startMouseY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingArrow) {
            draggingArrow = false;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}