package com.fun.client.render;

import com.fun.client.util.ConfigManager;
import com.fun.client.util.HudConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ThinkConfigScreen extends Screen {
    private final Screen parent;
    private static final String[] SOUND_KEYS = {"pling", "orb", "bit", "bell"};

    public ThinkConfigScreen(Screen parent) {
        super(Component.translatable("think.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 50;
        int btnW = 150, btnH = 20, gap = 24;

        // 1. HUD 开关
        this.addRenderableWidget(Button.builder(
                toggleText("think.config.hud", HudConfig.hudVisible),
                b -> {
                    HudConfig.hudVisible = !HudConfig.hudVisible;
                    b.setMessage(toggleText("think.config.hud", HudConfig.hudVisible));
                    ConfigManager.save();
                }
        ).bounds(centerX - btnW - 5, centerY, btnW, btnH).build());

        // 2. 音效开关
        this.addRenderableWidget(Button.builder(
                toggleText("think.config.sound", HudConfig.soundEnabled),
                b -> {
                    HudConfig.soundEnabled = !HudConfig.soundEnabled;
                    b.setMessage(toggleText("think.config.sound", HudConfig.soundEnabled));
                    ConfigManager.save();
                }
        ).bounds(centerX + 5, centerY, btnW, btnH).build());

        // 3. Y 轴提示开关
        this.addRenderableWidget(Button.builder(
                toggleText("think.config.yaxis", HudConfig.showYAxis),
                b -> {
                    HudConfig.showYAxis = !HudConfig.showYAxis;
                    b.setMessage(toggleText("think.config.yaxis", HudConfig.showYAxis));
                    ConfigManager.save();
                }
        ).bounds(centerX - btnW - 5, centerY + gap, btnW, btnH).build());

        // 4. 实体警告开关
        this.addRenderableWidget(Button.builder(
                toggleText("think.config.warn", HudConfig.showEntityWarning),
                b -> {
                    HudConfig.showEntityWarning = !HudConfig.showEntityWarning;
                    b.setMessage(toggleText("think.config.warn", HudConfig.showEntityWarning));
                    ConfigManager.save();
                }
        ).bounds(centerX + 5, centerY + gap, btnW, btnH).build());

        // 5. 警告音效选择器（循环切换）
        this.addRenderableWidget(Button.builder(
                soundText(),
                b -> {
                    int idx = 0;
                    for (int i = 0; i < SOUND_KEYS.length; i++) {
                        if (SOUND_KEYS[i].equals(HudConfig.warningSound)) { idx = i; break; }
                    }
                    HudConfig.warningSound = SOUND_KEYS[(idx + 1) % SOUND_KEYS.length];
                    b.setMessage(soundText());
                    ConfigManager.save();
                }
        ).bounds(centerX - btnW - 5, centerY + gap * 2, btnW * 2 + 10, btnH).build());

        // 6. 调整警告范围
        this.addRenderableWidget(Button.builder(
                Component.translatable("think.config.range", (int)HudConfig.detectRange, (int)HudConfig.warningRange, (int)HudConfig.dangerRange),
                b -> {
                    if (HudConfig.detectRange == 16) {
                        HudConfig.detectRange = 24; HudConfig.warningRange = 12; HudConfig.dangerRange = 6;
                    } else if (HudConfig.detectRange == 24) {
                        HudConfig.detectRange = 32; HudConfig.warningRange = 16; HudConfig.dangerRange = 8;
                    } else {
                        HudConfig.detectRange = 16; HudConfig.warningRange = 8; HudConfig.dangerRange = 4;
                    }
                    b.setMessage(Component.translatable("think.config.range", (int)HudConfig.detectRange, (int)HudConfig.warningRange, (int)HudConfig.dangerRange));
                    ConfigManager.save();
                }
        ).bounds(centerX - btnW - 5, centerY + gap * 3, btnW * 2 + 10, btnH).build());

        // 7. 打开 HUD 位置编辑
        this.addRenderableWidget(Button.builder(
                Component.translatable("think.config.editor"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new HudEditorScreen());
                    }
                }
        ).bounds(centerX - btnW - 5, centerY + gap * 4, btnW * 2 + 10, btnH).build());

        // 8. 完成/返回
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(this.parent);
                    }
                }
        ).bounds(centerX - btnW / 2, centerY + gap * 5 + 10, btnW, btnH).build());
    }

    private Component toggleText(String key, boolean state) {
        return Component.translatable(key).append(": ")
                .append(Component.translatable(state ? "think.state.on" : "think.state.off"));
    }

    private Component soundText() {
        return Component.translatable("think.config.sound_type")
                .append(": ")
                .append(Component.translatable("think.sound." + HudConfig.warningSound));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 20, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}