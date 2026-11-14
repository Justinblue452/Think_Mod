package com.fun.client.util;

import com.fun.client.render.HudEditorScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class KeyBindings {
    private static final KeyMapping.Category THINK_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("think", "main"));

    public static KeyMapping resetLocator, toggleHud, openEditor, startLocate;

    public static void register() {
        resetLocator = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.reset", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, THINK_CATEGORY));
        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.toggleHud", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, THINK_CATEGORY));
        openEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.editor", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, THINK_CATEGORY));
        startLocate = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.startLocate", InputConstants.Type.KEYBOARD, InputConstants.KEY_L, THINK_CATEGORY));
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
    }
}