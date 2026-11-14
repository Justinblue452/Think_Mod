package com.fun.client.render;

import com.fun.client.util.ConfigManager;
import com.fun.client.util.HudConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private boolean draggingArrow = false;
    private boolean draggingInfo = false;

    // 记录拖拽开始时的鼠标位置和 HUD 初始位置
    private double startMouseX, startMouseY;
    private int startArrowX, startArrowY;
    private int startInfoX, startInfoY;

    public HudEditorScreen() {
        super(Component.translatable("think.screen.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        var client = this.minecraft;
        if (client == null || client.player == null) return;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        g.fill(0, 0, screenW, screenH, 0x60000000);

        // 绘制箭头区域
        int ax = screenW / 2 + HudConfig.arrowX;
        int ay = screenH - HudConfig.arrowY;
        g.fill(ax - 20, ay - 20, ax + 20, ay + 20, draggingArrow ? 0x80FFFF00 : 0x40FFFFFF);
        g.text(client.font, Component.translatable("think.screen.arrow"), ax - 12, ay - 5, 0xFFFFFFFF, false);

        // 绘制信息面板区域
        int ix = HudConfig.infoX;
        int iy = screenH - HudConfig.infoY;
        g.fill(ix - 10, iy - 10, ix + 110, iy + 60, draggingInfo ? 0x80FFFF00 : 0x40FFFFFF);
        g.text(client.font, Component.translatable("think.screen.info"), ix, iy + 20, 0xFFFFFFFF, false);

        g.text(client.font, Component.translatable("think.screen.hint"), screenW / 2 - 100, 20, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        var client = this.minecraft;
        if (client == null) return false;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        double mouseX = event.x();
        double mouseY = event.y();

        // 判定箭头区域
        int ax = screenW / 2 + HudConfig.arrowX;
        int ay = screenH - HudConfig.arrowY;
        if (Math.abs(mouseX - ax) < 25 && Math.abs(mouseY - ay) < 25) {
            draggingArrow = true;
            // 记录初始状态
            startMouseX = mouseX;
            startMouseY = mouseY;
            startArrowX = HudConfig.arrowX;
            startArrowY = HudConfig.arrowY;
            return true;
        }

        // 判定信息面板区域
        int ix = HudConfig.infoX;
        int iy = screenH - HudConfig.infoY;
        if (mouseX >= ix - 15 && mouseX <= ix + 120 && mouseY >= iy - 15 && mouseY <= iy + 70) {
            draggingInfo = true;
            // 记录初始状态
            startMouseX = mouseX;
            startMouseY = mouseY;
            startInfoX = HudConfig.infoX;
            startInfoY = HudConfig.infoY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        // 使用绝对坐标偏移法，避免小浮点数被强转为 int 变成 0 的截断问题
        double currentMouseX = event.x();
        double currentMouseY = event.y();

        if (draggingArrow) {
            double deltaX = currentMouseX - startMouseX;
            double deltaY = currentMouseY - startMouseY;

            HudConfig.arrowX = startArrowX + (int) deltaX;
            // 屏幕 Y 轴向下，而 arrowY 是距离底部的距离，所以取反
            HudConfig.arrowY = startArrowY - (int) deltaY;
            return true;
        }

        if (draggingInfo) {
            double deltaX = currentMouseX - startMouseX;
            double deltaY = currentMouseY - startMouseY;

            HudConfig.infoX = startInfoX + (int) deltaX;
            HudConfig.infoY = startInfoY - (int) deltaY;
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingArrow || draggingInfo) {
            draggingArrow = false;
            draggingInfo = false;
            // 实时保存，防止玩家异常退出丢失
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