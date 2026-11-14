package com.fun.client.util;

import com.fun.client.render.HudEditorScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class KeyBindings {
    private static final KeyMapping.Category THINK_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("think", "main"));

    public static KeyMapping resetLocator, toggleHud, openEditor, startLocate, showCoords, togglePortalTarget;

    public static void register() {
        resetLocator = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.reset", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, THINK_CATEGORY));
        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.toggleHud", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, THINK_CATEGORY));
        openEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.editor", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, THINK_CATEGORY));
        startLocate = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.startLocate", InputConstants.Type.KEYBOARD, InputConstants.KEY_L, THINK_CATEGORY));
        showCoords = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.showCoords", InputConstants.Type.KEYBOARD, InputConstants.KEY_P, THINK_CATEGORY));
        togglePortalTarget = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.togglePortalTarget", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, THINK_CATEGORY));
    }

    public static void tick() {
        while (resetLocator.consumeClick()) StrongholdLocator.reset();
        while (toggleHud.consumeClick()) HudConfig.hudVisible = !HudConfig.hudVisible;
        while (openEditor.consumeClick()) {
            Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
        }
        while (startLocate.consumeClick()) {
            StrongholdLocator.startSession();
        }
        while (showCoords.consumeClick()) {
            showTranslatedCoords();
        }
        while (togglePortalTarget.consumeClick()) {
            togglePortalTarget();
        }
    }

    /** P 键：动作栏输出异世界对应坐标 */
    private static void showTranslatedCoords() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        var pos = client.player.blockPosition();

        if (client.level.dimension() == Level.OVERWORLD) {
            var n = CoordinateUtil.overworldToNether(pos);
            client.player.sendOverlayMessage(Component.translatable("think.portal.toNether",
                    n.getX(), n.getY(), n.getZ()));
        } else if (client.level.dimension() == Level.NETHER) {
            var o = CoordinateUtil.netherToOverworld(pos);
            client.player.sendOverlayMessage(Component.translatable("think.portal.toOverworld",
                    o.getX(), o.getY(), o.getZ()));
        } else {
            client.player.sendOverlayMessage(Component.translatable("think.portal.wrongDim"));
        }
    }

    /** O 键：设置/清除传送门目标 */
    private static void togglePortalTarget() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (PortalAssistant.hasTarget()) {
            PortalAssistant.clearTarget();
            client.player.sendOverlayMessage(Component.translatable("think.portal.cleared"));
        } else {
            var pos = client.player.blockPosition();

            // 修复：使用极其稳定的原版常量判断
            String dim = "minecraft:overworld";
            if (client.level.dimension() == net.minecraft.world.level.Level.NETHER) {
                dim = "minecraft:the_nether";
            } else if (client.level.dimension() == net.minecraft.world.level.Level.END) {
                dim = "minecraft:the_end";
            }

            PortalAssistant.setTarget(pos.getX(), pos.getZ(), dim);
            client.player.sendOverlayMessage(Component.translatable("think.portal.set",
                    pos.getX(), pos.getZ()));
        }
        ConfigManager.save();
    }
}